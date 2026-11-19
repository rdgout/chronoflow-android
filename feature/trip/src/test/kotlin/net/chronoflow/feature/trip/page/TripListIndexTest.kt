package net.chronoflow.feature.trip.page

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TripListIndexTest {
    @Test fun headersTimelinesAndEndHaveStablePositions() {
        val index = TripListIndex(listOf("a", "b", "c"), false)
        assertEquals(listOf(2, 4, 6), listOf("a", "b", "c").map(index::headerIndex))
        assertEquals(listOf(3, 5, 7), listOf("a", "b", "c").map(index::timelineIndex))
        assertEquals(8, index.end)
        assertNull(index.hidden)
        assertEquals(9, index.itemCount)
        assertEquals("b", index.dayAt(5))
        assertNull(index.dayAt(0))
        assertNull(index.dayAt(8))
    }

    @Test fun hiddenItemFollowsTheEnd() {
        val index = TripListIndex(listOf("a", "b", "c"), true)
        assertEquals(9, index.hidden)
        assertEquals(10, index.itemCount)
    }

    @Test(expected = IllegalArgumentException::class)
    fun unknownDayThrows() {
        TripListIndex(emptyList(), false).headerIndex("missing")
    }
}
