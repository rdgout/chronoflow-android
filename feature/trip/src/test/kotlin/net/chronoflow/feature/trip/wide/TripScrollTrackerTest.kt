package net.chronoflow.feature.trip.wide

import net.chronoflow.core.plan.trippage.TripPageLayout
import net.chronoflow.feature.trip.page.PageIndex
import net.chronoflow.feature.trip.page.TripListIndex
import net.chronoflow.feature.trip.wide.TripScrollTracker.ItemSample
import net.chronoflow.feature.trip.wide.TripScrollTracker.Restore
import net.chronoflow.feature.trip.wide.TripScrollTracker.SizeKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TripScrollTrackerTest {
    private val tracker = TripScrollTracker()
    private val index = TripListIndex(listOf("a", "b"), false)
    private val phone = SizeKey(411, 2.625f, TripPageLayout.resolve(411.0), true)
    private val wide = SizeKey(841, 2.625f, TripPageLayout.resolve(841.0), true)
    private val items = listOf(ItemSample("t:a", "dayTimeline", -472))

    private fun observe(
        key: SizeKey = phone,
        samples: List<ItemSample> = items,
        inProgress: Boolean = false,
        oldIndex: PageIndex = index,
        newIndex: PageIndex = index,
        start: Int = 0,
    ): Restore? = tracker.onSize(key, samples, start, oldIndex, newIndex, 94, inProgress)

    @Test fun firstObservationNeverRestores() {
        assertNull(observe())
    }

    @Test fun heightOnlyChangeNeverRestores() {
        observe()
        assertNull(observe(phone.copy()))
    }

    @Test fun widthChangeRestoresTheDayAndOffset() {
        observe()
        assertEquals(Restore(3, 472), observe(wide))
        assertEquals(472 / 2.625, tracker.lastCaptured!!.offsetDp, 0.0001)
    }

    @Test fun densityChangeConvertsThroughDp() {
        observe()
        assertEquals(Restore(3, 539), observe(phone.copy(density = 3f)))
    }

    @Test fun dayJustBelowTheTopKeepsANegativeOffset() {
        val samples = listOf(ItemSample("h:a", "dayHeader", 20), ItemSample("t:a", "dayTimeline", 200))
        observe(samples = samples)
        assertEquals(Restore(2, -20), observe(wide, samples))
        assertEquals(true, tracker.lastCaptured!!.onHeader)
    }

    @Test fun summaryPositionDoesNotRestore() {
        val samples = listOf(ItemSample("summary", "summary", -100), ItemSample("h:a", "dayHeader", 95))
        observe(samples = samples)
        assertNull(observe(wide, samples))
    }

    @Test fun scrollInProgressSkips() {
        observe()
        assertNull(observe(wide, inProgress = true))
        assertNull(observe(wide))
    }

    @Test fun cancelRestorationDropsThePendingRequest() {
        observe()
        observe(wide)
        tracker.cancelRestoration()
        assertNull(tracker.lastCaptured)
        assertNull(observe(wide))
    }

    @Test fun modeChangeLandsOnTheDayTop() {
        observe(phone.copy(timelineMode = false))
        assertEquals(Restore(2, 0), observe(wide))
    }

    @Test fun phoneViewToggleDoesNotOverwriteTheRequestedDayWithTheOldListPosition() {
        val rows = net.chronoflow.feature.trip.list.ListTestRows.days
        val old =
            net.chronoflow.feature.trip.list
                .ListModeIndex(rows.map { it.dayId }, rows, false)
        val next = TripListIndex(rows.map { it.dayId }, false)
        val opening = listOf(ItemSample("h:${rows[1].dayId}", "dayHeader", 0))
        observe(phone.copy(timelineMode = false), opening, oldIndex = old)
        tracker.cancelRestoration()

        // The toggle requests a timeline index before the old list's item table has been replaced.
        val transient = listOf(ItemSample(rows[0].rows[0].key, "listRow", 0))
        assertNull(observe(phone, transient, oldIndex = old, newIndex = next))
        assertNull(tracker.lastCaptured)
    }

    @Test fun phoneTimelineToListToggleDoesNotRestoreTheOldTimelinePosition() {
        observe()
        tracker.cancelRestoration()
        assertNull(observe(phone.copy(timelineMode = false)))
        assertNull(tracker.lastCaptured)
    }

    @Test fun unknownDayDoesNotRestore() {
        observe()
        assertNull(observe(wide, newIndex = TripListIndex(listOf("b"), false)))
    }

    @Test fun viewportStartOffsetBelongsToTheOldLayout() {
        observe(start = -24)
        assertEquals(Restore(3, 448), observe(wide, start = -24))
    }

    @Test fun aListRowResolvesThroughTheOldIndexWithoutParsingItsIds() {
        val rows = net.chronoflow.feature.trip.list.ListTestRows.days
        val old =
            net.chronoflow.feature.trip.list
                .ListModeIndex(rows.map { it.dayId }, rows, false)
        val day = rows[0]
        val samples = listOf(ItemSample(day.rows[0].key, "listRow", -100))
        val next = TripListIndex(rows.map { it.dayId }, false)
        observe(phone.copy(timelineMode = false), samples, oldIndex = old)
        assertEquals(Restore(next.headerIndex(day.dayId), 0), observe(wide, samples, oldIndex = old, newIndex = next))
    }
}
