package net.chronoflow.feature.trip.timeline.host

import androidx.compose.foundation.lazy.LazyListItemInfo

/**
 * Index math of the page's items: per day a sticky header at `2 * i` and the day at `2 * i + 1`, then the end item.
 * It never searches the plan.
 */
class TimelineListIndex(
    private val dayIds: List<String>,
) {
    private val positions =
        HashMap<String, Int>(dayIds.size * 2).also { map ->
            dayIds.forEachIndexed { i, id ->
                map[id] =
                    i
            }
        }

    val dayCount: Int get() = dayIds.size

    val endIndex: Int get() = 2 * dayIds.size

    fun position(dayId: String): Int? = positions[dayId]

    fun headerIndex(dayId: String): Int = 2 * checkNotNull(positions[dayId]) { "unknown day $dayId" }

    fun dayIndex(dayId: String): Int = headerIndex(dayId) + 1

    /** The day number of the last day item (content type `"day"`) whose top is at or above [thresholdPx], else null. */
    fun currentDayIndex(
        visible: List<LazyListItemInfo>,
        thresholdPx: Int,
    ): Int? {
        var current: Int? = null
        for (item in visible) {
            if (item.contentType == DAY_CONTENT_TYPE && item.offset <= thresholdPx) current = (item.index - 1) / 2
        }
        return current
    }

    companion object {
        const val DAY_CONTENT_TYPE = "day"
        const val HEADER_CONTENT_TYPE = "dayHeader"
        const val END_CONTENT_TYPE = "end"
        const val END_KEY = "end"

        fun headerKey(dayId: String): String = "h:$dayId"
    }
}
