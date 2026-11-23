package net.chronoflow.app.reminders

import net.chronoflow.core.data.reminders.ACTION_OPEN
import net.chronoflow.core.data.reminders.EXTRA_BLOCK_ID
import net.chronoflow.core.data.reminders.EXTRA_TRIP_ID
import net.chronoflow.core.navigation.ReminderTapExtras
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderTapIntentPolicyTest {
    private val extras = mapOf(EXTRA_TRIP_ID to "trip", EXTRA_BLOCK_ID to "light")

    @Test fun validOpenActionCapturesTheTap() {
        assertEquals(
            ReminderTapExtras("trip", "light"),
            ReminderTapIntentPolicy.tapToCapture(ACTION_OPEN, 0, false, extras::get),
        )
    }

    @Test fun wrongActionRestoredHistoryAndMalformedTripAreIgnored() {
        assertNull(ReminderTapIntentPolicy.tapToCapture("other", 0, false, extras::get))
        assertNull(ReminderTapIntentPolicy.tapToCapture(ACTION_OPEN, 0, true, extras::get))
        assertNull(ReminderTapIntentPolicy.tapToCapture(ACTION_OPEN, 0x00100000, false, extras::get))
        assertNull(ReminderTapIntentPolicy.tapToCapture(ACTION_OPEN, 0, false) { "a/b" })
    }
}
