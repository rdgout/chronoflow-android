package net.chronoflow.core.testing

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.BlockView
import net.chronoflow.core.model.contracts.DayPhase
import net.chronoflow.core.model.contracts.DayView
import net.chronoflow.core.model.contracts.Lane
import net.chronoflow.core.model.contracts.TripCardView
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.plan.planDate
import java.security.MessageDigest
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/** Port of iOS `MockTimelineFixtures`: opt-in 30 and 60 day multi-zone stress trips built from S3 and S1. */
object LargeTimelineFixtures {
    data class Fixture(
        val card: TripCardView,
        val detail: TripDetail,
    )

    val large30: Fixture by lazy { large(30) }
    val large60: Fixture by lazy { large(60) }

    private val bytes = ConcurrentHashMap<String, ByteArray>()

    fun fixture(named: String): Fixture {
        if (named == "large30") return large30
        if (named == "large60") return large60
        val templates = MockTripTemplates.templates()
        val index = if (named.startsWith("S")) named.drop(1).toIntOrNull()?.minus(1) else null
        val template =
            index?.let { templates.getOrNull(it) } ?: templates.firstOrNull { it.card.id == named }
                ?: throw IllegalArgumentException("unknown timeline fixture: $named")
        return Fixture(template.card, template.detail)
    }

    fun detailBytes(named: String): ByteArray =
        bytes.getOrPut(named) {
            ApiJson.response.encodeToString(TripDetail.serializer(), fixture(named).detail).encodeToByteArray()
        }

    private fun large(count: Int): Fixture {
        val templates = MockTripTemplates.templates()
        val template = templates[2]
        val source = template.detail
        val cycle = source.plan.days.take(6)
        val start = instant(cycle[0].startUtc)
        val end = instant(cycle.last().endUtc)
        val duration = end.epochSecond - start.epochSecond
        val sourceBlocks = source.plan.blocks.filter { instant(it.startUtc) < end && instant(it.endUtc) >= start }
        val sourceDayIDs = cycle.map { it.id }.toSet()
        val sourceBlockIDs = sourceBlocks.map { it.id }.toSet()
        val days = ArrayList<DayView>()
        val blocks = ArrayList<BlockView>()
        val timelineDays = ArrayList<net.chronoflow.core.model.contracts.TimelineDayPresentationView>()
        val blockReasons = ArrayList<net.chronoflow.core.model.contracts.BlockReasonTagsView>()
        val transferWarnings = ArrayList<net.chronoflow.core.model.contracts.TransferWarningsView>()
        val noticeDays = ArrayList<net.chronoflow.core.model.contracts.DayNoticesView>()
        for (index in 0 until count / cycle.size) {
            val suffix = "-tour-$index"
            val shift = duration * index
            val clone = shifted(TripDetail.serializer(), source, shift, suffix, sourceDayIDs, sourceBlockIDs)
            days += clone.plan.days.filter { it.id.dropLast(suffix.length) in sourceDayIDs }
            blocks +=
                clone.plan.blocks
                    .filter { it.id.dropLast(suffix.length) in sourceBlockIDs }
                    .map {
                        it.copy(
                            startUtc = iso(maxOf(instant(it.startUtc), start.plusSeconds(shift))),
                            endUtc = iso(minOf(instant(it.endUtc), end.plusSeconds(shift))),
                        )
                    }
            timelineDays += clone.timeline.days.filter { it.dayId.dropLast(suffix.length) in sourceDayIDs }
            blockReasons += clone.timeline.blockReasons.filter { it.blockId.dropLast(suffix.length) in sourceBlockIDs }
            transferWarnings +=
                clone.timeline.transferWarnings.filter { it.blockId.dropLast(suffix.length) in sourceBlockIDs }
            noticeDays += clone.notices.days.filter { it.dayId.dropLast(suffix.length) in sourceDayIDs }
        }
        val caffeinePlan = templates[0].detail.plan
        val caffeineDay =
            caffeinePlan.days.first { day ->
                day.phase == DayPhase.Post &&
                    caffeinePlan.blocks.count { it.lane == Lane.Caffeine && startsIn(it, day) } >= 3
            }
        val caffeine = caffeinePlan.blocks.filter { it.lane == Lane.Caffeine && startsIn(it, caffeineDay) }
        val caffeineIDs = caffeine.map { it.id }.toSet()
        for ((index, day) in days.withIndex().toList()) {
            val shift = instant(day.startUtc).epochSecond - instant(caffeineDay.startUtc).epochSecond
            val dayEnd = instant(day.endUtc)
            blocks +=
                shifted(
                    ListSerializer(BlockView.serializer()),
                    caffeine,
                    shift,
                    "-caffeine-day-$index",
                    emptySet(),
                    caffeineIDs,
                ).map {
                    it.copy(
                        startUtc = iso(minOf(instant(it.startUtc), dayEnd)),
                        endUtc = iso(minOf(instant(it.endUtc), dayEnd)),
                        displayTz = day.zones.first().tz,
                    )
                }
        }
        val id = "0199a3c0-0000-7000-8000-0000000000$count"
        val hash = sha256Hex("timeline-$count-S3-prefix6-S1-caffeine-v2")
        val plan = source.plan
        val result =
            source.copy(
                trip = source.trip.copy(id = id, endUtc = days.last().endUtc),
                plan =
                    plan.copy(
                        days = days,
                        blocks = blocks,
                        meta = plan.meta.copy(inputHash = hash, assumedReturn = null),
                    ),
                notices = source.notices.copy(days = noticeDays),
                timeline =
                    source.timeline.copy(
                        days = timelineDays,
                        blockReasons = blockReasons,
                        transferWarnings = transferWarnings,
                    ),
                checkState = source.checkState.copy(planInputHash = hash, marks = emptyList(), days = emptyList()),
                detailVersion = hash,
            )
        return Fixture(template.card.copy(id = id), result)
    }

    private fun startsIn(
        block: BlockView,
        day: DayView,
    ): Boolean {
        val at = instant(block.startUtc)
        return at >= instant(day.startUtc) && at < instant(day.endUtc)
    }

    /** Encodes, rewrites every JSON string (ids get the suffix, instants and dates move by [seconds]) and decodes again. */
    private fun <T> shifted(
        serializer: KSerializer<T>,
        value: T,
        seconds: Long,
        suffix: String,
        dayIDs: Set<String>,
        blockIDs: Set<String>,
    ): T {
        fun transform(element: JsonElement): JsonElement =
            when (element) {
                is JsonObject -> {
                    JsonObject(element.mapValues { (_, child) -> transform(child) })
                }

                is JsonArray -> {
                    JsonArray(element.map(::transform))
                }

                is JsonPrimitive -> {
                    if (element.isString) {
                        JsonPrimitive(
                            rewrite(element.content, seconds, suffix, dayIDs, blockIDs),
                        )
                    } else {
                        element
                    }
                }
            }
        val encoded = ApiJson.response.encodeToJsonElement(serializer, value)
        return ApiJson.response.decodeFromJsonElement(serializer, transform(encoded))
    }

    private fun rewrite(
        string: String,
        seconds: Long,
        suffix: String,
        dayIDs: Set<String>,
        blockIDs: Set<String>,
    ): String {
        if (string in dayIDs || string in blockIDs) return string + suffix
        if (string.length >= 17 && 'T' in string && string.endsWith("Z")) {
            parse(string)?.let { return iso(it.plusSeconds(seconds)) }
        }
        if (string.length == 10) {
            parse(string + "T12:00:00Z")?.let { return iso(it.plusSeconds(seconds)).take(10) }
        }
        return string
    }

    private val strict = Regex("""^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z$""")

    /** `ISO8601DateFormatter` defaults: a 17 character `...THH:MMZ` gets its seconds, anything else must be exact. */
    private fun parse(string: String): Instant? {
        val text = if (string.length == 17) string.replace("Z", ":00Z") else string
        return if (strict.matches(text)) planDate(text) else null
    }

    private fun instant(string: String): Instant =
        requireNotNull(parse(string)) { "fixture instant '$string' does not parse" }

    private fun iso(instant: Instant): String = instant.toString()

    private fun sha256Hex(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.encodeToByteArray()).joinToString("") {
            val value = it.toInt() and 0xff
            (if (value < 16) "0" else "") + value.toString(16)
        }
}
