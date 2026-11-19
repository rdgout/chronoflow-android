package net.chronoflow.feature.trip.page

import androidx.compose.foundation.lazy.LazyListItemInfo
import net.chronoflow.core.plan.trippage.TripPageAnchor

fun currentDay(
    visible: List<LazyListItemInfo>,
    index: PageIndex,
    thresholdPx: Int,
): String? =
    visible
        .lastOrNull {
            (
                it.contentType == "dayHeader" || it.contentType == "dayTimeline" ||
                    it.contentType == "listRow" || it.contentType == "zoneRow"
            ) && it.offset <= thresholdPx
        }?.let { index.dayAt(it.index) }

fun headerBottomPx(
    visible: List<LazyListItemInfo>,
    firstVisibleIndex: Int,
    headerIndex: Int,
): Float =
    visible.firstOrNull { it.index == headerIndex }?.let { (it.offset + it.size).toFloat() }
        ?: if (headerIndex < firstVisibleIndex) Float.NEGATIVE_INFINITY else Float.POSITIVE_INFINITY

fun compactAlpha(
    bottomPx: Float,
    fadePx: Float,
    reduceMotion: Boolean,
): Float =
    if (reduceMotion) {
        if (bottomPx <= 0f) 1f else 0f
    } else {
        (1f - bottomPx / fadePx).coerceIn(0f, 1f)
    }

fun headerScrolled(
    visible: List<LazyListItemInfo>,
    firstVisibleIndex: Int,
    headerIndex: Int,
): Boolean = headerIndex < firstVisibleIndex || visible.any { it.index == headerIndex && it.offset < 0 }

fun offsetInDayPx(
    visible: List<LazyListItemInfo>,
    timelineIndex: Int,
): Int = visible.firstOrNull { it.index == timelineIndex }?.offset?.let { (-it).coerceAtLeast(0) } ?: 0

fun restoreTarget(
    index: TripListIndex,
    anchor: TripPageAnchor,
    offsetPx: Int,
): Pair<Int, Int> {
    val id = anchor.dayID ?: return index.summary to 0
    return if (offsetPx > 0) index.timelineIndex(id) to offsetPx else index.headerIndex(id) to 0
}
