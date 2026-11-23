package net.chronoflow.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderTapTests {
    @Test fun capturedJSONAndTripStep() {
        val account = "01900000-0000-7000-8000-000000000001"
        assertEquals(
            ReminderTapExtras("trip-id", "light", account, "hash"),
            ReminderTapExtras.parse("trip-id", "light", account, "hash"),
        )
        assertNull(ReminderTapExtras.parse(null, null, null, null))
        assertNull(ReminderTapExtras.parse("", null, null, null))
        assertNull(ReminderTapExtras.parse("a/b", null, null, null))
        assertNull(ReminderTapExtras.parse("a?x=1", null, null, null))
        assertNull(ReminderTapExtras.parse("a#b", null, null, null))
        assertEquals(ReminderTapExtras("t"), ReminderTapExtras.parse("t", null, null, null))
    }
}
