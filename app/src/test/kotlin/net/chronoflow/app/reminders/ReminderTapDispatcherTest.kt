package net.chronoflow.app.reminders

import kotlinx.coroutines.test.runTest
import net.chronoflow.core.navigation.PendingLaunchIntent
import net.chronoflow.core.navigation.ReminderTapExtras
import net.chronoflow.core.navigation.ReminderTapRouter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class ReminderTapDispatcherTest {
    @Test fun reminderIsDeliveredToTheRouter() =
        runTest(timeout = 60.seconds) {
            val router = ReminderTapRouter()
            val tap = ReminderTapExtras("trip", "light")
            ReminderTapDispatcher(router).handle(PendingLaunchIntent(reminder = tap))
            assertEquals(tap, router.pending.value)
        }

    @Test fun linksOnlyLeaveTheRouterEmpty() =
        runTest(timeout = 60.seconds) {
            val router = ReminderTapRouter()
            ReminderTapDispatcher(router).handle(PendingLaunchIntent(links = setOf("https://app.chronoflow.net/x")))
            assertNull(router.pending.value)
        }
}
