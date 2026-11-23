package net.chronoflow.feature.trip.timeline

import androidx.compose.foundation.lazy.LazyListItemInfo
import net.chronoflow.feature.trip.timeline.host.TimelineListIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimelineListIndexTest {
    private val index = TimelineListIndex(listOf("a", "b", "c"))

    private class Item(
        override val index: Int,
        override val offset: Int,
        override val contentType: Any?,
    ) : LazyListItemInfo {
        override val key: Any = index
        override val size: Int = 100
    }

    private fun day(
        dayNumber: Int,
        offset: Int,
    ) = Item(2 * dayNumber + 1, offset, TimelineListIndex.DAY_CONTENT_TYPE)

    private fun header(
        dayNumber: Int,
        offset: Int,
    ) = Item(2 * dayNumber, offset, TimelineListIndex.HEADER_CONTENT_TYPE)

    @Test fun headerDayAndEndIndices() {
        assertEquals(listOf(0, 2, 4), listOf("a", "b", "c").map(index::headerIndex))
        assertEquals(listOf(1, 3, 5), listOf("a", "b", "c").map(index::dayIndex))
        assertEquals(6, index.endIndex)
    }

    @Test fun aDayItemAtTheThresholdIsCurrentAndOnePixelBelowIsNot() {
        assertEquals(1, index.currentDayIndex(listOf(header(1, 0), day(1, 36), header(2, 400), day(2, 436)), 36))
        assertEquals(0, index.currentDayIndex(listOf(header(0, -60), day(0, -24), header(1, 0), day(1, 37)), 36))
    }

    @Test fun theLastQualifyingDayWins() {
        assertEquals(2, index.currentDayIndex(listOf(day(0, -300), day(1, -10), day(2, 20)), 36))
    }

    @Test fun headersAndTheEndItemAreNotDays() {
        assertNull(index.currentDayIndex(listOf(header(0, 0), Item(6, 0, TimelineListIndex.END_CONTENT_TYPE)), 36))
    }
}
