package net.chronoflow.core.datamock

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.BlockParams
import net.chronoflow.core.model.contracts.TripCardView
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.testing.Template
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object MockTripFixtures {
    private val utcPattern = Regex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2})?Z$")
    private val localPattern = Regex("^\\d{4}-\\d{2}-\\d{2}(T\\d{2}:\\d{2})?$")
    private val localFormat = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm", Locale.ROOT)

    fun rebase(
        fixture: Template,
        days: Int,
        minutes: Int = 0,
    ): Template {
        fun transform(
            value: JsonElement,
            zone: String,
        ): JsonElement =
            when (value) {
                is JsonObject -> {
                    val zones = value["zones"] as? JsonArray
                    val displayZone =
                        value["displayTz"].string ?: value["tz"].string
                            ?: (zones?.firstOrNull() as? JsonObject)?.get("tz").string ?: zone
                    val from = value["from"] as? JsonObject
                    val to = value["to"] as? JsonObject
                    val legs = value["legs"] as? JsonArray
                    val lastArrival = (legs?.lastOrNull() as? JsonObject)?.get("to") as? JsonObject
                    JsonObject(
                        value.mapValues { (key, entry) ->
                            if (key == "markedAt") {
                                entry
                            } else {
                                val fieldZone =
                                    when (key) {
                                        "departureUtc", "departLocal" -> {
                                            from?.get("zone").string ?: displayZone
                                        }

                                        "arrivalUtc", "arriveLocal" -> {
                                            to?.get("zone").string ?: displayZone
                                        }

                                        "endUtc" -> {
                                            lastArrival?.get("zone").string
                                                ?: (zones?.lastOrNull() as? JsonObject)?.get("tz").string ?: displayZone
                                        }

                                        "arrivalLandingUtc", "assumedReturn" -> {
                                            fixture.card.to.zone
                                        }

                                        else -> {
                                            displayZone
                                        }
                                    }
                                transform(entry, fieldZone)
                            }
                        },
                    )
                }

                is JsonArray -> {
                    JsonArray(value.map { transform(it, zone) })
                }

                else -> {
                    val text = value.string
                    when {
                        text == null -> {
                            value
                        }

                        utcPattern.matches(text) -> {
                            val shifted = instant(text).atZone(ZoneId.of(zone)).plusDays(days.toLong())
                            JsonPrimitive(iso(shifted.toInstant().plusSeconds(minutes * 60L)))
                        }

                        localPattern.matches(text) -> {
                            if ('T' in text) {
                                val date =
                                    LocalDateTime
                                        .parse(
                                            text,
                                            localFormat,
                                        ).atZone(ZoneId.of(zone))
                                        .withLaterOffsetAtOverlap()
                                        .plusDays(days.toLong())
                                        .plusMinutes(minutes.toLong())
                                JsonPrimitive(localFormat.format(date))
                            } else {
                                JsonPrimitive(LocalDate.parse(text).plusDays(days.toLong()).toString())
                            }
                        }

                        else -> {
                            value
                        }
                    }
                }
            }
        var card =
            ApiJson.response.decodeFromJsonElement(
                TripCardView.serializer(),
                transform(
                    ApiJson.response.encodeToJsonElement(TripCardView.serializer(), fixture.card),
                    fixture.card.from.zone,
                ),
            )
        var detail =
            ApiJson.response.decodeFromJsonElement(
                TripDetail.serializer(),
                transform(
                    ApiJson.response.encodeToJsonElement(TripDetail.serializer(), fixture.detail),
                    fixture.card.from.zone,
                ),
            )
        val rebasedDays =
            detail.plan.days.map { value ->
                val label = local(value.startUtc, value.zones.first().tz).take(10)
                val last = local(iso(instant(value.endUtc).minusSeconds(1)), value.zones.last().tz).take(10)
                value.copy(labelDate = label, labelRange = if (value.labelRange != null && last > label) last else null)
            }
        val airports =
            listOf(card.from, card.to) + card.connections +
                detail.trip.legs.flatMap { listOf(it.from, it.to) }
        val blocks =
            detail.plan.blocks.map { block ->
                val key = block.checkKey?.copy(localDate = local(block.startUtc, block.displayTz).take(10))
                val params = block.params
                val from =
                    (params as? BlockParams.ZoneChange)?.value?.departureIata?.let { iata ->
                        airports.firstOrNull {
                            it.iata ==
                                iata
                        }
                    }
                val to =
                    (params as? BlockParams.ZoneChange)?.value?.arrivalIata?.let { iata ->
                        airports.firstOrNull {
                            it.iata ==
                                iata
                        }
                    }
                val updatedParams =
                    if (params is BlockParams.ZoneChange && from != null && to != null) {
                        val at = instant(block.startUtc)
                        BlockParams.ZoneChange(
                            params.value.copy(offsetMin = (offset(to.zone, at) - offset(from.zone, at)) / 60),
                        )
                    } else {
                        params
                    }
                block.copy(checkKey = key, params = updatedParams)
            }
        val marks =
            detail.checkState.marks.mapIndexed { index, mark ->
                val key =
                    fixture.detail.checkState.marks[index]
                        .key
                val blockIndex =
                    fixture.detail.plan.blocks
                        .indexOfFirst { it.checkKey == key }
                mark.copy(key = blocks.getOrNull(blockIndex)?.checkKey ?: mark.key)
            }
        val hash = sha256Hex("${fixture.detail.plan.meta.inputHash}:$days" + if (minutes == 0) "" else ":$minutes")
        val difference =
            (offset(card.to.zone, instant(card.arrivalUtc)) - offset(card.from.zone, instant(card.departureUtc))) / 60
        card =
            card.copy(
                departLocal = local(card.departureUtc, card.from.zone),
                arriveLocal = local(card.arrivalUtc, card.to.zone),
                offsetDifferenceMinutes = difference,
            )
        detail =
            detail.copy(
                plan =
                    detail.plan.copy(
                        days = rebasedDays,
                        blocks = blocks,
                        meta = detail.plan.meta.copy(inputHash = hash),
                    ),
                checkState = detail.checkState.copy(planInputHash = hash, marks = marks),
                detailVersion = "${fixture.detail.detailVersion}-$hash",
                trip =
                    detail.trip.copy(
                        departLocal = local(detail.trip.departureUtc, detail.trip.from.zone),
                        offsetDifferenceMin = difference,
                    ),
                timeline =
                    detail.timeline.copy(
                        days =
                            detail.timeline.days.map { day ->
                                day.copy(
                                    ticks =
                                        day.ticks.map {
                                            it.copy(
                                                offsetMin =
                                                    offset(it.tz, instant(it.atUtc)) / 60,
                                            )
                                        },
                                )
                            },
                    ),
            )
        return Template(card, detail)
    }

    internal fun local(
        utc: String,
        zone: String,
    ): String = localFormat.format(instant(utc).atZone(ZoneId.of(zone)))

    internal fun offset(
        zone: String,
        at: Instant,
    ): Int =
        ZoneId
            .of(zone)
            .rules
            .getOffset(at)
            .totalSeconds
}
