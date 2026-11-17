package net.chronoflow.core.plan

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.BlockView
import net.chronoflow.core.model.contracts.CopyLine
import net.chronoflow.core.model.contracts.DayView
import net.chronoflow.core.model.contracts.DayZoneView
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.PlanNoticesView
import net.chronoflow.core.model.contracts.PlanView
import net.chronoflow.core.model.contracts.TimelinePresentationView
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.model.store.PlanVersionRecord
import net.chronoflow.core.testing.Fixtures
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Serializable
data class PlanFixture(
    val plan: PlanView,
    val timeline: TimelinePresentationView,
    val notices: PlanNoticesView,
)

val testCatalog: CopyCatalog by lazy {
    CopyCatalog(checkNotNull(CopyCatalog::class.java.getResourceAsStream("/plan_copy.json")).use { it.readBytes() })
}

private val fixtureCache = HashMap<String, PlanFixture>()

fun fixture(name: String = "S1"): PlanFixture =
    synchronized(fixtureCache) {
        fixtureCache.getOrPut(name) {
            ApiJson.response.decodeFromString(PlanFixture.serializer(), Fixtures.text("plans/$name.json"))
        }
    }

private val isoMillis =
    DateTimeFormatter
        .ofPattern(
            "uuuu-MM-dd'T'HH:mm:ss.SSS'Z'",
            Locale.ROOT,
        ).withZone(ZoneOffset.UTC)

fun at(minute: Double): String = isoMillis.format(Instant.ofEpochMilli(((1_767_225_600 + minute * 60) * 1000).toLong()))

fun sampleDay(hours: Double = 24.0): DayView {
    val start = at(0.0)
    val end = at(hours * 60)
    return fixture().plan.days[0].copy(
        startUtc = start,
        endUtc = end,
        zones = listOf(DayZoneView("Europe/Amsterdam", start, end)),
    )
}

fun block(
    id: String,
    start: Double,
    end: Double,
): BlockView =
    fixture()
        .plan.blocks
        .first { it.type == BlockType.Sleep }
        .copy(id = id, startUtc = at(start), endUtc = at(end))

fun unresolvedCopyLines(
    element: JsonElement,
    label: String,
    out: MutableList<String>,
) {
    when (element) {
        is JsonObject -> {
            val key = (element["key"] as? JsonPrimitive)?.takeIf { it.isString }?.content
            if (key != null && element["args"] is JsonArray) {
                val line =
                    try {
                        ApiJson.response.decodeFromJsonElement(CopyLine.serializer(), element)
                    } catch (_: SerializationException) {
                        null
                    } catch (_: IllegalArgumentException) {
                        null
                    }
                if (line != null) {
                    if (!testCatalog.contains(key)) out += "$label: $key"
                    for (language in listOf(Language.En, Language.Nl)) {
                        if (CopyLineFormatter(testCatalog).format(line, CopyFormat(language)).isEmpty()) {
                            out +=
                                "$label: $key (${language.raw}) is empty"
                        }
                    }
                }
            }
            element.values.forEach { unresolvedCopyLines(it, label, out) }
        }

        is JsonArray -> {
            element.forEach { unresolvedCopyLines(it, label, out) }
        }

        else -> {}
    }
}

fun tripDetail(): TripDetail =
    ApiJson.response.decodeFromString(TripDetail.serializer(), Fixtures.text("trips/detail.json"))

fun recordOf(value: TripDetail): PlanVersionRecord =
    PlanVersionRecord(
        value.trip.id,
        value.plan.meta.inputHash,
        ApiJson.response.encodeToString(TripDetail.serializer(), value).encodeToByteArray(),
        "now",
        value.detailVersion,
    )
