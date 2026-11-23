package net.chronoflow.feature.trip.page

import androidx.compose.foundation.lazy.LazyListItemInfo
import net.chronoflow.core.plan.trippage.TripPageAnchor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripScrollMathTest {
    private val index = TripListIndex(listOf("a", "b", "c"), false)

    private data class Item(
        override val index: Int,
        override val offset: Int,
        override val size: Int = 90,
        override val contentType: Any? = "dayHeader",
        override val key: Any = index,
    ) : LazyListItemInfo

    @Test fun currentDayChangesAtTheScaledThreshold() {
        assertEquals("b", currentDay(listOf(Item(3, -200, contentType = "dayTimeline"), Item(4, 36)), index, 36))
        assertEquals("a", currentDay(listOf(Item(3, -200, contentType = "dayTimeline"), Item(4, 37)), index, 36))
    }

    @Test fun summaryAndPillsKeepTheAnchor() {
        assertNull(
            currentDay(listOf(Item(0, 0, contentType = "summary"), Item(1, 20, contentType = "dayNav")), index, 36),
        )
    }

    @Test fun headerBottomDistinguishesAboveAndBelowTheViewport() {
        assertEquals(80f, headerBottomPx(listOf(Item(2, -10)), 2, 2))
        assertEquals(Float.NEGATIVE_INFINITY, headerBottomPx(emptyList(), 3, 2))
        assertEquals(Float.POSITIVE_INFINITY, headerBottomPx(emptyList(), 0, 2))
    }

    @Test fun compactAlphaFadesOnlyOverTheFinalDistance() {
        assertEquals(0f, compactAlpha(24f, 24f, false))
        assertEquals(0.5f, compactAlpha(12f, 24f, false))
        assertEquals(1f, compactAlpha(0f, 24f, false))
        assertEquals(1f, compactAlpha(-5f, 24f, false))
        assertEquals(0f, compactAlpha(1f, 24f, true))
        assertEquals(1f, compactAlpha(0f, 24f, true))
    }

    @Test fun headerScrolledChangesOnlyAtTheBoundary() {
        assertTrue(headerScrolled(listOf(Item(2, -1)), 2, 2))
        assertFalse(headerScrolled(listOf(Item(2, 0)), 2, 2))
        assertTrue(headerScrolled(emptyList(), 3, 2))
    }

    @Test fun offsetBelongsToTheTimelineAndRestoresToItsItem() {
        assertEquals(360, offsetInDayPx(listOf(Item(3, -360)), 3))
        assertEquals(0, offsetInDayPx(listOf(Item(2, -40)), 3))
        assertEquals(2 to 0, restoreTarget(index, TripPageAnchor("a"), 0))
        assertEquals(3 to 120, restoreTarget(index, TripPageAnchor("a"), 120))
    }
}
