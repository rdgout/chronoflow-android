package net.chronoflow.app.reminders

import net.chronoflow.core.navigation.ReminderTapExtras
import net.chronoflow.core.navigation.ReminderTapRouter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderTapLaunchTests {
    @Test fun coldIntentPreservesEndingAndWarmRouterCanReopen() {
        val tap = ReminderTapExtras("trip-id", "light", inputHash = "hash")
        val router = ReminderTapRouter()
        router.receive(tap)
        assertEquals("light", router.pending.value?.blockId)
        router.clear()
        assertNull(router.pending.value)
        router.receive(tap)
        assertEquals("trip-id", router.pending.value?.tripId)
    }
}
