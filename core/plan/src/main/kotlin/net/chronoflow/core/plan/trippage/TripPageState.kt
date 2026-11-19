package net.chronoflow.core.plan.trippage

import net.chronoflow.core.model.contracts.BlockView
import net.chronoflow.core.model.contracts.DayView
import net.chronoflow.core.plan.planDate
import java.time.Instant
import kotlin.math.abs

enum class TripPageFetch { Idle, Loading, Loaded, Offline, NotFound, Failed }

enum class TripPageState {
    Loading,
    RemoteLoading,
    RemoteOffline,
    Ready,
    NoPlan,
    NotFound,
    Server,
    ;

    companion object {
        fun resolve(
            keepOffline: Boolean,
            stored: Boolean,
            noPlan: Boolean = false,
            online: Boolean,
            fetch: TripPageFetch,
            missingAfterSync: Boolean = false,
            tombstoned: Boolean = false,
            awaitingSync: Boolean = false,
        ): TripPageState {
            if (tombstoned) return NotFound
            if (stored) return if (noPlan) NoPlan else Ready
            if (missingAfterSync && !awaitingSync) return NotFound
            if (keepOffline) return Loading
            if (!online || fetch == TripPageFetch.Offline) return RemoteOffline
            return when (fetch) {
                TripPageFetch.NotFound -> NotFound
                TripPageFetch.Failed -> Server
                else -> RemoteLoading
            }
        }
    }
}

data class TripPageAnchor(
    val dayID: String? = null,
    val labelDate: String? = null,
    val offset: Double = 0.0,
)

private fun DayView.contains(at: Instant): Boolean {
    val start = planDate(startUtc) ?: return false
    val end = planDate(endUtc) ?: return false
    return start <= at && at < end
}

fun openingDay(
    days: List<DayView>,
    blocks: List<BlockView>,
    requestedDay: String?,
    blockID: String?,
    keptDay: String?,
    now: Instant,
): DayView? {
    if (requestedDay != null) days.firstOrNull { it.id == requestedDay }?.let { return it }
    if (blockID != null) {
        val start = blocks.firstOrNull { it.id == blockID }?.let { planDate(it.startUtc) }
        if (start != null) days.firstOrNull { it.contains(start) }?.let { return it }
    }
    if (keptDay != null) days.firstOrNull { it.id == keptDay }?.let { return it }
    days.firstOrNull { it.contains(now) }?.let { return it }
    val last = days.lastOrNull()
    if (last != null) {
        val end = planDate(last.endUtc)
        if (end != null && now >= end) return last
    }
    return days.firstOrNull()
}

fun replacementAnchor(
    anchor: TripPageAnchor,
    days: List<DayView>,
): TripPageAnchor {
    val date = anchor.labelDate
    val day =
        if (date == null) {
            null
        } else {
            days.firstOrNull { it.id == anchor.dayID && it.labelDate == date }
                ?: days.firstOrNull { it.labelDate == date }
                ?: nearest(days, date)
        }
    if (day == null) return TripPageAnchor(days.firstOrNull()?.id, days.firstOrNull()?.labelDate)
    return TripPageAnchor(day.id, day.labelDate, anchor.offset)
}

private fun nearest(
    days: List<DayView>,
    date: String,
): DayView? {
    val target = planDate("${date}T12:00:00Z") ?: return days.firstOrNull()
    var best: DayView? = null
    var bestDistance = Double.POSITIVE_INFINITY
    for (day in days) {
        val at = planDate("${day.labelDate}T12:00:00Z")
        val distance =
            if (at ==
                null
            ) {
                Double.POSITIVE_INFINITY
            } else {
                abs((at.epochSecond - target.epochSecond).toDouble())
            }
        if (best == null || distance < bestDistance) {
            best = day
            bestDistance = distance
        }
    }
    return best
}
