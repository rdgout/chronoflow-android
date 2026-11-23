@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.core.data.reminders

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.testing.reminders.reminderAccount
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class ReminderDeliveryGateTest {
    @Test fun signOutCannotLeaveAPostWhoseFinalAccountReadWasAlreadyInFlight() =
        runTest(timeout = 60.seconds) {
            val gate = ReminderDeliveryGate()
            val reading = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            var account: String? = reminderAccount
            val notices = mutableListOf<String>()
            val posting =
                backgroundScope.async {
                    gate.postIfCurrent(reminderAccount, {
                        val snapshot = account
                        reading.complete(Unit)
                        release.await()
                        snapshot
                    }) { notices += reminderAccount }
                }
            reading.await()
            val signingOut =
                backgroundScope.launch {
                    gate.beginWipe { account = null }
                    notices.clear()
                }
            runCurrent()
            release.complete(Unit)
            posting.await()
            signingOut.join()
            assertTrue("a reminder survived completed sign-out", notices.isEmpty())
        }

    @Test fun wipeBeforeTheFinalAccountReadRejectsThePost() =
        runTest(timeout = 60.seconds) {
            val gate = ReminderDeliveryGate()
            var account: String? = reminderAccount
            var posted = false
            gate.beginWipe { account = null }
            assertFalse(gate.postIfCurrent(reminderAccount, { account }) { posted = true })
            assertFalse(posted)
        }

    @Test fun cancellationWhileReadingReleasesTheGateForSignOut() =
        runTest(timeout = 60.seconds) {
            val gate = ReminderDeliveryGate()
            val reading = CompletableDeferred<Unit>()
            val posting =
                backgroundScope.launch {
                    gate.postIfCurrent(reminderAccount, {
                        reading.complete(Unit)
                        awaitCancellation()
                    }) { error("cancelled delivery posted") }
                }
            reading.await()
            posting.cancelAndJoin()
            var wiped = false
            gate.beginWipe { wiped = true }
            assertTrue(wiped)
        }
}
