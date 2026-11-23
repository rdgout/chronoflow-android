package net.chronoflow.feature.trip.wide

import net.chronoflow.core.plan.trippage.TripPageLayout
import net.chronoflow.feature.trip.page.PageIndex
import kotlin.math.roundToInt

// Android's width-independent day height permits one request before measure, without the iOS display-link loop.
class TripScrollTracker {
    data class SizeKey(
        val windowWidthDp: Int,
        val density: Float,
        val layout: TripPageLayout,
        val timelineMode: Boolean,
    )

    data class ItemSample(
        val key: Any,
        val contentType: Any?,
        val offsetPx: Int,
    )

    data class Restore(
        val index: Int,
        val offsetPx: Int,
    )

    data class Captured(
        val dayId: String,
        val onHeader: Boolean,
        val offsetDp: Double,
    )

    private var previous: SizeKey? = null
    var lastCaptured: Captured? = null
        private set

    fun onSize(
        key: SizeKey,
        previousItems: List<ItemSample>,
        viewportStartOffsetPx: Int,
        oldIndex: PageIndex,
        newIndex: PageIndex,
        thresholdPx: Int,
        scrollInProgress: Boolean,
    ): Restore? {
        val old = previous
        previous = key
        if (old == null || old == key) return null
        lastCaptured = null
        // A phone toggle owns its scroll request; its old item table can already have been remeasured.
        if (old.windowWidthDp == key.windowWidthDp && old.density == key.density &&
            old.layout == key.layout
        ) {
            return null
        }
        if (scrollInProgress) return null
        val current =
            previousItems.lastOrNull {
                it.contentType in DAY_TYPES && it.offsetPx - viewportStartOffsetPx <= thresholdPx
            } ?: return null
        val dayId = oldIndex.dayForKey(current.key) ?: return null
        if (newIndex.dayForKey("h:$dayId") == null) return null
        if (!old.timelineMode || old.timelineMode != key.timelineMode) {
            lastCaptured = Captured(dayId, onHeader = true, offsetDp = 0.0)
            return Restore(newIndex.headerIndex(dayId), 0)
        }
        val timeline = previousItems.firstOrNull { it.key == "t:$dayId" && it.offsetPx <= viewportStartOffsetPx }
        val item = timeline ?: previousItems.firstOrNull { it.key == "h:$dayId" } ?: return null
        val onHeader = timeline == null
        val offset = -(item.offsetPx - viewportStartOffsetPx).toDouble() / old.density
        lastCaptured = Captured(dayId, onHeader, offset)
        val index = if (onHeader) newIndex.headerIndex(dayId) else newIndex.timelineIndex(dayId)
        return Restore(index, (offset * key.density).roundToInt())
    }

    fun cancelRestoration() {
        lastCaptured = null
    }

    private companion object {
        val DAY_TYPES = setOf("dayHeader", "dayTimeline", "listRow", "zoneRow")
    }
}
