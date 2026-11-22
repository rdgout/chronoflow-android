@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.app.reminders

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.app.shell.ShellViewModel
import net.chronoflow.core.model.session.SessionSnapshot
import net.chronoflow.core.model.session.StoredSession
import net.chronoflow.core.navigation.ReminderTapExtras
import net.chronoflow.core.navigation.ReminderTapRouter
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.navigation.shell.ShellNav
import net.chronoflow.core.navigation.shell.ShellTab
import net.chronoflow.core.testing.reminders.reminderAccount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class ReminderTapRoutingTest {
    @Test fun tapDoesNotWaitForTheComposeDispatcherToAdvance() =
        runTest(timeout = 60.seconds) {
            val router = ReminderTapRouter()
            val holder = ShellViewModel(ShellNav.initial(ShellTab.Today))
            val tap = ReminderTapExtras("trip", "light", reminderAccount, "hash")
            backgroundScope.launch(StandardTestDispatcher(testScheduler), start = CoroutineStart.UNDISPATCHED) {
                ReminderTapRouting(
                    router,
                    { SessionSnapshot(StoredSession("test", reminderAccount), 1) },
                    holder::openReminder,
                    UnconfinedTestDispatcher(testScheduler),
                ).run()
            }
            router.receive(tap)
            assertEquals(ShellTab.Trips, holder.state.selected)
            val page =
                holder.state.stacks
                    .getValue(ShellTab.Trips)
                    .last() as Route.TripPage
            assertEquals(tap.tripId, page.tripId)
            assertEquals(tap.blockId, page.blockId)
            assertEquals(tap.inputHash, page.notificationInputHash)
            assertNull(router.pending.value)
        }

    @Test fun matchingAccountOpensAndClearsBeforeTheAsyncCheck() = checkRouting(reminderAccount, true, true)

    @Test fun legacyTapWithoutAnAccountOpens() = checkRouting(null, true, true)

    @Test fun otherAccountIsClearedWithoutOpening() = checkRouting("other", true, false)

    @Test fun signedOutTapIsClearedWithoutOpening() = checkRouting(reminderAccount, false, false)

    private fun checkRouting(
        account: String?,
        signedIn: Boolean,
        shouldOpen: Boolean,
    ) = runTest(timeout = 60.seconds) {
        val router = ReminderTapRouter()
        val tap = ReminderTapExtras("trip", "light", account, "hash")
        val opened = mutableListOf<ReminderTapExtras>()
        router.receive(tap)
        backgroundScope.launch {
            ReminderTapRouting(router, {
                assertNull(router.pending.value)
                if (signedIn) SessionSnapshot(StoredSession("test", reminderAccount), 1) else null
            }, opened::add, StandardTestDispatcher(testScheduler)).run()
        }
        runCurrent()
        assertNull(router.pending.value)
        assertEquals(if (shouldOpen) listOf(tap) else emptyList<ReminderTapExtras>(), opened)
    }
}
