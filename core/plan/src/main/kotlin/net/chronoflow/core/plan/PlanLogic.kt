package net.chronoflow.core.plan

import net.chronoflow.core.model.contracts.BlockParams
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.BlockView
import net.chronoflow.core.model.contracts.DayStatus
import net.chronoflow.core.model.contracts.DayView
import net.chronoflow.core.model.contracts.LabelPhase
import net.chronoflow.core.model.contracts.NoteRole
import net.chronoflow.core.model.contracts.NoteView
import net.chronoflow.core.model.contracts.PlanNoticesView
import net.chronoflow.core.model.contracts.PlanView
import net.chronoflow.core.model.contracts.Priority
import net.chronoflow.core.model.contracts.SleepKind
import net.chronoflow.core.model.contracts.TripCardView
import net.chronoflow.core.model.contracts.TripDetailView
import net.chronoflow.core.model.contracts.TripStatus
import java.time.Instant

fun isOptionalStep(block: BlockView): Boolean =
    when (block.type) {
        BlockType.AvoidLight, BlockType.SeekLight -> block.priority == Priority.Optional
        BlockType.CaffeineOK -> true
        BlockType.LightOK, BlockType.LightNeutral -> block.priority == Priority.Info
        else -> false
    }

fun withoutOptionalSteps(plan: PlanView): PlanView = plan.copy(blocks = plan.blocks.filterNot(::isOptionalStep))

fun tripDisplayStatus(
    card: TripCardView,
    planStartUtc: String?,
    planEndUtc: String?,
    now: Instant,
): TripStatus {
    val status = card.progress.status
    if (status == TripStatus.Completed || planStartUtc == null || planEndUtc == null) return status
    val end = planDate(planEndUtc)
    if (end != null && now >= end) return TripStatus.Completed
    val start = planDate(planStartUtc)
    return if (start != null && start <= now) TripStatus.Active else TripStatus.Upcoming
}

data class NoticeEntry(
    val note: NoteView,
    val warning: Boolean,
)

fun noticeItems(
    plan: PlanView,
    notices: PlanNoticesView,
    day: DayView,
): List<NoticeEntry> {
    val indexes =
        notices.days
            .firstOrNull { it.dayId == day.id }
            ?.warningIndexes
            .orEmpty()
    val warnings = indexes.mapNotNull { plan.warnings.getOrNull(it)?.let { note -> NoticeEntry(note, warning = true) } }
    val entries = ArrayList<NoticeEntry>()
    for (entry in warnings + day.notes.map { NoticeEntry(it, it.role == NoteRole.Warning) }) {
        val index = entries.indexOfFirst { it.note == entry.note }
        if (index >= 0) {
            entries[index] = entries[index].copy(warning = entries[index].warning || entry.warning)
        } else {
            entries.add(entry)
        }
    }
    return entries
}

fun dayZoneLabel(
    day: DayView,
    trip: TripDetailView? = null,
): String =
    day.zones
        .map { it.tz }
        .distinct()
        .joinToString(" → ") { zoneLabel(it, trip) }

data class DisplayInterval(
    val start: Instant,
    val end: Instant,
    val tz: String,
)

fun summaryIntervals(
    day: DayView,
    blocks: List<BlockView>,
    type: BlockType,
): List<DisplayInterval> {
    val dayStart = planDate(day.startUtc)
    val dayEnd = planDate(day.endUtc)
    val intervals = ArrayList<DisplayInterval>()
    for (block in blocks) {
        if (block.type != type || isOptionalStep(block)) continue
        if (type == BlockType.Sleep) {
            val params = block.params as? BlockParams.Sleep ?: continue
            if (params.value.kind != SleepKind.Main && params.value.kind != SleepKind.InFlightMain) continue
        }
        val blockStart = planDate(block.startUtc)
        val blockEnd = planDate(block.endUtc)
        if (dayStart == null || dayEnd == null || blockStart == null || blockEnd == null) continue
        val start = maxOf(dayStart, blockStart)
        val end = minOf(dayEnd, blockEnd)
        if (start >= end) continue
        for (zone in day.zones) {
            val zoneStart = planDate(zone.startUtc) ?: continue
            val zoneEnd = planDate(zone.endUtc) ?: continue
            val from = maxOf(start, zoneStart)
            val until = minOf(end, zoneEnd)
            if (from < until) intervals.add(DisplayInterval(from, until, zone.tz))
        }
    }
    val merged = ArrayList<DisplayInterval>()
    for (interval in intervals.sortedBy { it.start }) {
        val last = merged.lastOrNull()
        if (last != null && last.tz == interval.tz && last.end >= interval.start) {
            merged[merged.size - 1] = last.copy(end = maxOf(last.end, interval.end))
        } else {
            merged.add(interval)
        }
    }
    return merged
}

fun stepLabelKey(type: BlockType): String? =
    when (type) {
        BlockType.Sleep -> "Sleep"
        BlockType.Nap -> "Nap"
        BlockType.Rest -> "Rest"
        BlockType.SeekLight -> "Seek bright light"
        BlockType.LightOK -> "Normal light is fine"
        BlockType.LightNeutral -> "Indoor light"
        BlockType.AvoidLight -> "Avoid bright light"
        BlockType.CaffeineServing -> "Caffeine serving"
        BlockType.CaffeineRoutine -> "Caffeine routine"
        BlockType.CaffeineOK -> "Caffeine window"
        BlockType.NoCaffeine -> "Avoid caffeine"
        BlockType.Flight -> "Flight"
        BlockType.Layover -> "Layover"
        BlockType.Transfer -> "Airport transfer"
        BlockType.MovementBreak -> "Movement break"
        BlockType.ZoneChange -> "Time-zone change"
        BlockType.Note -> "Note"
        else -> null
    }

fun dayLabelKey(phase: LabelPhase): String? =
    when (phase) {
        LabelPhase.PreDeparture -> "Before departure"
        LabelPhase.Travel -> "Travel day"
        LabelPhase.TravelArrivalNight -> "Travel · arrival night"
        LabelPhase.ArrivalDay0 -> "Arrival day"
        LabelPhase.Day -> "Day :count"
        else -> null
    }

fun dayStatusLabelKey(status: DayStatus): String? =
    when (status) {
        DayStatus.Adapting -> "Adjusting"
        DayStatus.PartiallyAdapted -> "Partly adapted"
        DayStatus.Adapted -> "Expected adapted"
        DayStatus.AdaptedSlowerClock -> "Protect a slower clock"
        else -> null
    }
