package net.chronoflow.feature.trip.wide

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.ui.unit.IntSize
import net.chronoflow.core.plan.trippage.TripWideListPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class WidePinSourceTest {
    private class Item(
        override val offset: Int,
        override val key: Any = "t:a",
    ) : LazyListItemInfo {
        override val index = 3
        override val size = 2000
        override val contentType = "dayTimeline"
    }

    private class Info(
        override val visibleItemsInfo: List<LazyListItemInfo>,
        override val viewportStartOffset: Int = 0,
    ) : LazyListLayoutInfo {
        override val viewportEndOffset = 800
        override val totalItemsCount = 4
        override val viewportSize = IntSize(1000, 800)
    }

    @Test fun viewportStartOffsetShiftsTheDayTop() {
        val pin =
            WidePinSource { Info(listOf(Item(-100)), -24) }.apply {
                stickyTopPx = 36
                maxHeightPx = 700
            }
        assertEquals(112, pin.position("t:a", 2000, 900).offsetPx)
    }

    @Test fun invisibleDayHasNoViewport() {
        val pin = WidePinSource { Info(listOf(Item(0))) }.apply { maxHeightPx = 700 }
        assertEquals(0, pin.position("t:other", 2000, 900).heightPx)
    }

    @Test fun bottomInsetReducesTheViewport() {
        val pin =
            WidePinSource { Info(listOf(Item(-100))) }.apply {
                stickyTopPx = 36
                bottomInsetPx = 100
                maxHeightPx = 700
            }
        assertEquals(664, pin.position("t:a", 2000, 900).heightPx)
    }

    @Test fun capIsScaledAndThePxFormulaMatchesThePort() {
        val density = 2.625
        val pin =
            WidePinSource { Info(listOf(Item(-100))) }.apply {
                stickyTopPx = 94
                maxHeightPx = (700 * density).toInt()
            }
        val actual = pin.position("t:a", 2500, 2400)
        val expected = TripWideListPosition(-100 / density, 2500 / density, 800 / density, 94 / density, 2400 / density)
        assertEquals(expected.offset * density, actual.offsetPx.toDouble(), 0.001)
        assertEquals(expected.height * density, actual.heightPx.toDouble(), 0.001)
        assertSame(actual, pin.position("t:a", 2500, 2400))
    }
}
