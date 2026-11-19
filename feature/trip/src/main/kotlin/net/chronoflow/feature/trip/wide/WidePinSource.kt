package net.chronoflow.feature.trip.wide

import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable

class WidePinPosition {
    var offsetPx: Int = 0
        internal set
    var heightPx: Int = 0
        internal set
}

@Stable
class WidePinSource(
    private val layoutInfo: () -> LazyListLayoutInfo,
) {
    constructor(listState: LazyListState) : this({ listState.layoutInfo })

    var stickyTopPx: Int = 0
    var bottomInsetPx: Int = 0
    var maxHeightPx: Int = 0
    private val result = WidePinPosition()

    fun position(
        itemKey: String,
        dayHeightPx: Int,
        contentHeightPx: Int,
    ): WidePinPosition {
        val info = layoutInfo()
        val items = info.visibleItemsInfo
        var i = 0
        while (i < items.size) {
            val item = items[i]
            if (item.key == itemKey) {
                val dayTop = item.offset - info.viewportStartOffset
                val viewportHeight = info.viewportSize.height - bottomInsetPx
                val offset = (stickyTopPx - dayTop).coerceIn(0, dayHeightPx.coerceAtLeast(0))
                result.offsetPx = offset
                result.heightPx =
                    minOf(
                        dayHeightPx - offset,
                        minOf(viewportHeight - stickyTopPx, minOf(maxHeightPx, contentHeightPx)),
                    ).coerceAtLeast(0)
                return result
            }
            i++
        }
        result.offsetPx = 0
        result.heightPx = 0
        return result
    }
}
