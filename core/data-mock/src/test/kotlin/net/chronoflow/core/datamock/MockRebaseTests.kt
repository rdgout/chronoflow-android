package net.chronoflow.core.datamock

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.BlockParams
import net.chronoflow.core.model.contracts.CopyArg
import net.chronoflow.core.model.contracts.CopyArgKind
import net.chronoflow.core.model.contracts.CopyArgValue
import net.chronoflow.core.model.contracts.CopyArgument
import net.chronoflow.core.model.contracts.TripDetail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.time.ZoneId

class MockRebaseTests {
    /** A local copy argument starts at the later occurrence before adding elapsed minutes (new). */
    @Test fun localCopyTimeUsesLaterAmsterdamOverlap() =
        localCopyTimeUsesLaterOffset(
            "2026-10-25T02:30",
            "Europe/Amsterdam",
            "2026-10-25T03:30",
        )

    /** The same formatter policy applies to the New York overlap (new). */
    @Test fun localCopyTimeUsesLaterNewYorkOverlap() =
        localCopyTimeUsesLaterOffset(
            "2026-11-01T01:30",
            "America/New_York",
            "2026-11-01T02:30",
        )

    private fun localCopyTimeUsesLaterOffset(
        local: String,
        zone: String,
        expected: String,
    ) {
        val fixture = MockAppSeed.decodedFixtures.trips.first()
        val block =
            fixture.detail.plan.blocks
                .first()
        val argument = CopyArgument.Standard(CopyArg("time", CopyArgKind.Text, CopyArgValue.Text(local), zone))
        val detail =
            fixture.detail.copy(
                plan =
                    fixture.detail.plan.copy(
                        blocks =
                            listOf(block.copy(text = block.text.copy(args = listOf(argument)))) +
                                fixture.detail.plan.blocks
                                    .drop(1),
                    ),
            )
        val shifted = MockTripFixtures.rebase(fixture.copy(detail = detail), days = 0, minutes = 60)
        val shiftedArgument =
            shifted.detail.plan.blocks
                .first()
                .text.args
                .single() as CopyArgument.Standard
        assertEquals(CopyArgValue.Text(expected), shiftedArgument.value.value)
    }

    companion object {
        @JvmStatic @BeforeClass
        fun warmFixtures() {
            warmMockFixtures()
        }
    }

    @Test fun preservesWallTimesAcrossAmsterdamAndNewYorkDST20261025() =
        preservesWallTimesAcrossAmsterdamAndNewYorkDST("2026-10-25T12:00:00Z")

    @Test fun preservesWallTimesAcrossAmsterdamAndNewYorkDST20261101() =
        preservesWallTimesAcrossAmsterdamAndNewYorkDST("2026-11-01T12:00:00Z")

    @Test fun preservesWallTimesAcrossAmsterdamAndNewYorkDST20261107() =
        preservesWallTimesAcrossAmsterdamAndNewYorkDST("2026-11-07T12:00:00Z")

    private fun preservesWallTimesAcrossAmsterdamAndNewYorkDST(nowText: String) {
        val now = instant(nowText)
        val changedOffsets = mutableSetOf<String>()

        fun expectWallTime(
            old: String,
            new: String,
            zone: String,
        ) {
            val before = instant(old).atZone(ZoneId.of(zone))
            val after = instant(new).atZone(ZoneId.of(zone))
            assertEquals("wall time in $zone", before.toLocalTime(), after.toLocalTime())
            if (before.offset != after.offset) changedOffsets += zone
        }

        fun expectCopyInstants(
            old: JsonElement,
            new: JsonElement,
        ) {
            if (old is JsonObject && new is JsonObject) {
                if (old["kind"].string == "instant" && old["tz"].string != null && old["value"].string != null &&
                    new["value"].string != null
                ) {
                    expectWallTime(old["value"].string!!, new["value"].string!!, old["tz"].string!!)
                }
                for ((key, value) in old) new[key]?.let { expectCopyInstants(value, it) }
            } else if (old is JsonArray &&
                new is JsonArray
            ) {
                old.zip(new).forEach { (before, after) -> expectCopyInstants(before, after) }
            }
        }
        for ((index, old) in MockAppSeed.decodedFixtures.trips.withIndex()) {
            val new = MockAppSeed.rebase(old, index, now)
            expectCopyInstants(
                ApiJson.response.encodeToJsonElement(TripDetail.serializer(), old.detail),
                ApiJson.response.encodeToJsonElement(TripDetail.serializer(), new.detail),
            )
            expectWallTime(old.card.departureUtc, new.card.departureUtc, old.card.from.zone)
            expectWallTime(old.card.arrivalUtc, new.card.arrivalUtc, old.card.to.zone)
            assertEquals(old.card.departLocal.takeLast(5), new.card.departLocal.takeLast(5))
            assertEquals(old.card.arriveLocal.takeLast(5), new.card.arriveLocal.takeLast(5))
            expectWallTime(
                old.detail.trip.endUtc,
                new.detail.trip.endUtc,
                old.detail.trip.legs
                    .last()
                    .to.zone,
            )
            old.detail.plan.meta.arrivalLandingUtc?.let {
                expectWallTime(it, new.detail.plan.meta.arrivalLandingUtc!!, old.card.to.zone)
            }
            old.detail.plan.meta.assumedReturn?.let {
                expectWallTime(
                    it.leaveUtc,
                    new.detail.plan.meta.assumedReturn!!
                        .leaveUtc,
                    old.card.to.zone,
                )
            }
            for ((before, after) in old.detail.plan.days
                .zip(new.detail.plan.days)) {
                expectWallTime(before.startUtc, after.startUtc, before.zones.first().tz)
                expectWallTime(before.endUtc, after.endUtc, before.zones.last().tz)
                for ((first, second) in before.zones.zip(after.zones)) {
                    expectWallTime(first.startUtc, second.startUtc, first.tz)
                    expectWallTime(first.endUtc, second.endUtc, first.tz)
                }
            }
            for ((before, after) in old.detail.plan.blocks
                .zip(new.detail.plan.blocks)) {
                expectWallTime(before.startUtc, after.startUtc, before.displayTz)
                expectWallTime(before.endUtc, after.endUtc, before.displayTz)
                val oldParams = before.params as? BlockParams.CaffeineRoutine
                val newParams = after.params as? BlockParams.CaffeineRoutine
                if (oldParams?.value?.lastByUtc != null &&
                    newParams?.value?.lastByUtc != null
                ) {
                    expectWallTime(oldParams.value.lastByUtc!!, newParams.value.lastByUtc!!, before.displayTz)
                }
            }
            for ((before, after) in old.detail.timeline.days
                .flatMap {
                    it.ticks
                }.zip(
                    new.detail.timeline.days
                        .flatMap { it.ticks },
                )) {
                expectWallTime(before.atUtc, after.atUtc, before.tz)
            }
        }
        assertTrue("Amsterdam offset changed", "Europe/Amsterdam" in changedOffsets)
        if (nowText !=
            "2026-10-25T12:00:00Z"
        ) {
            assertTrue("New York offset changed", "America/New_York" in changedOffsets)
        }
    }
}
