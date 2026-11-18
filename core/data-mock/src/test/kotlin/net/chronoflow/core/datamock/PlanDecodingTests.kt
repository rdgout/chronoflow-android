package net.chronoflow.core.datamock

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.BlockParams
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.model.contracts.TripStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs

class PlanDecodingTests {
    companion object {
        @JvmStatic @BeforeClass
        fun warmFixtures() {
            warmMockFixtures()
        }
    }

    @Test fun mockTripsRebaseEveryTimestampAndDate20000229() =
        mockTripsRebaseEveryTimestampAndDate(
            "2000-02-29T23:59:00Z",
        )

    @Test fun mockTripsRebaseEveryTimestampAndDate20260329() =
        mockTripsRebaseEveryTimestampAndDate(
            "2026-03-29T01:30:00Z",
        )

    @Test fun mockTripsRebaseEveryTimestampAndDate20261025() =
        mockTripsRebaseEveryTimestampAndDate(
            "2026-10-25T01:30:00Z",
        )

    @Test fun mockTripsRebaseEveryTimestampAndDate20350715() =
        mockTripsRebaseEveryTimestampAndDate(
            "2035-07-15T12:00:00Z",
        )

    private fun mockTripsRebaseEveryTimestampAndDate(nowText: String) {
        val now = instant(nowText)
        val seed = MockAppSeed.make(now, zone = mockZone)
        val account = seed.accounts.getValue(MockScenarioValues.demoID)
        assertEquals(
            setOf(TripStatus.Active, TripStatus.Upcoming, TripStatus.Completed),
            account.trips
                .map {
                    it.card.progress.status
                }.toSet(),
        )
        for ((index, trip) in account.trips.withIndex()) {
            val old = MockAppSeed.decodedFixtures.trips[index]
            val detail = account.details.getValue(trip.card.id)
            val before = strings(ApiJson.response.encodeToJsonElement(TripDetail.serializer(), old.detail))
            val after = strings(ApiJson.response.encodeToJsonElement(TripDetail.serializer(), detail))
            val zone = ZoneId.of(trip.card.from.zone)
            val dayDelta =
                ChronoUnit.DAYS.between(
                    instant(old.card.departureUtc).atZone(zone).toLocalDate(),
                    instant(trip.card.departureUtc).atZone(zone).toLocalDate(),
                )
            for ((path, text) in before) {
                if (text.endsWith('Z') && text.contains('T') && after[path] != null) {
                    val elapsed = seconds(instant(after.getValue(path))) - seconds(instant(text))
                    assertTrue(path, abs(elapsed - dayDelta * 86400.0) <= 3600)
                } else if (!path.contains(
                        ".checkKey.",
                    ) && !path.endsWith(".labelDate") && !path.endsWith(".labelRange") &&
                    Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(text)
                ) {
                    assertEquals(path, LocalDate.parse(text).plusDays(dayDelta).toString(), after[path])
                }
            }
            assertEquals(old.card.from.zone, trip.card.from.zone)
            assertEquals(old.card.to.zone, trip.card.to.zone)
            assertEquals(trip.card.departLocal, detail.trip.departLocal)
            assertEquals(expectedLocal(trip.card.arrivalUtc, trip.card.to.zone), trip.card.arriveLocal)
            assertEquals(trip.planInputHash, detail.plan.meta.inputHash)
            assertEquals(trip.planInputHash, detail.checkState.planInputHash)
            assertNotEquals(old.detail.plan.meta.inputHash, detail.plan.meta.inputHash)
            assertNotEquals(old.detail.detailVersion, detail.detailVersion)
            for (day in detail.plan.days) {
                assertEquals(
                    expectedLocal(day.startUtc, day.zones.first().tz).take(10),
                    day.labelDate,
                )
            }
            for (block in detail.plan.blocks) {
                block.checkKey?.let {
                    assertEquals(
                        expectedLocal(block.startUtc, block.displayTz).take(10),
                        it.localDate,
                    )
                }
                val params = (block.params as? BlockParams.ZoneChange)?.value
                if (params != null) {
                    val airports = detail.trip.legs.flatMap { listOf(it.from, it.to) }
                    val from = airports.first { it.iata == params.departureIata }
                    val to = airports.first { it.iata == params.arrivalIata }
                    val at = instant(block.startUtc)
                    assertEquals(
                        (expectedOffset(to.zone, at) - expectedOffset(from.zone, at)) / 60,
                        params.offsetMin,
                    )
                }
            }
            for (tick in detail.timeline.days.flatMap { it.ticks }) {
                assertEquals(expectedOffset(tick.tz, instant(tick.atUtc)) / 60, tick.offsetMin)
            }
            when (trip.card.progress.status) {
                TripStatus.Active -> {
                    assertTrue(instant(trip.planStartUtc) <= now && now < instant(trip.planEndUtc))
                    assertTrue(trip.keepOffline)
                }

                TripStatus.Upcoming -> {
                    assertTrue(now < instant(trip.planStartUtc))
                }

                TripStatus.Completed -> {
                    assertTrue(now >= instant(trip.planEndUtc))
                    assertFalse(trip.keepOffline)
                }

                else -> {
                    error("unexpected mock status")
                }
            }
        }
    }

    private fun expectedLocal(
        utc: String,
        zone: String,
    ): String =
        java.time.format.DateTimeFormatter
            .ofPattern("uuuu-MM-dd'T'HH:mm", java.util.Locale.ROOT)
            .format(instant(utc).atZone(ZoneId.of(zone)))

    private fun expectedOffset(
        zone: String,
        at: java.time.Instant,
    ): Int =
        ZoneId
            .of(zone)
            .rules
            .getOffset(at)
            .totalSeconds

    private fun strings(
        value: JsonElement,
        path: String = "",
    ): Map<String, String> =
        when (value) {
            is JsonArray -> {
                value
                    .withIndex()
                    .flatMap { (i, entry) ->
                        strings(entry, "$path.$i").entries
                    }.associate { it.toPair() }
            }

            is JsonObject -> {
                value
                    .flatMap { (key, entry) ->
                        strings(entry, "$path.$key").entries
                    }.associate { it.toPair() }
            }

            else -> {
                value.string?.let { mapOf(path to it) } ?: emptyMap()
            }
        }
}
