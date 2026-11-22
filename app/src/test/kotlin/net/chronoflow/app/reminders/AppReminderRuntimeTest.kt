package net.chronoflow.app.reminders

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.data.reminders.ReminderDeliveryGate
import net.chronoflow.core.database.testing.TestRoots
import net.chronoflow.core.datastore.PermanentSessionKeyFailure
import net.chronoflow.core.datastore.WipeMarker
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.model.session.SessionStore
import net.chronoflow.core.model.session.StoredSession
import net.chronoflow.core.sync.session.MemorySessionStore
import net.chronoflow.core.testing.reminders.reminderAccount
import net.chronoflow.core.testing.reminders.reminderNow
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStoreException
import java.security.ProviderException
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

class AppReminderRuntimeTest {
    @Test fun finalPostChecksTheWipeMarkerAgain() =
        fixture { f ->
            f.credentials.data = StoredSession("test", reminderAccount).encode()
            assertEquals(reminderAccount, f.runtime.storedAccountId())
            f.deliveryGate.beginWipe { f.marker.write(AccountID.of(reminderAccount)) }
            assertFalse(f.runtime.postIfCurrent(reminderAccount) { fail("post ignored the wipe marker") })
        }

    @Test fun finalPostRejectsAnUnreadableAccount() =
        fixture { f ->
            f.sessionReadFailure = IOException()
            assertFalse(f.runtime.postIfCurrent(reminderAccount) { fail("post ignored a storage failure") })
        }

    @Test fun finalPostRunsForTheStoredAccount() =
        fixture { f ->
            f.credentials.data = StoredSession("test", reminderAccount).encode()
            var posted = false
            assertTrue(f.runtime.postIfCurrent(reminderAccount) { posted = true })
            assertTrue(posted)
        }

    @Test fun signedOutBackgroundPassDoesNotReschedule() =
        fixture { f ->
            f.runtime.backgroundPass()
            assertEquals(listOf("prepareBackground", "sessionGraphs", "session"), f.events)
        }

    @Test fun signedInPassStartsGraphsBeforeReadinessThenWaitsForAttachment() =
        fixture { f ->
            f.presence = SessionPresence.SignedIn(reminderAccount.uppercase(Locale.ROOT))
            f.attached.value = reminderAccount
            f.runtime.backgroundPass()
            assertEquals(listOf("prepareBackground", "sessionGraphs", "session", "reschedule"), f.events)
        }

    @Test fun wrongAccountAttachmentTimesOutWithoutScheduling() =
        fixture { f ->
            f.presence = SessionPresence.SignedIn(reminderAccount)
            f.attached.value = "other"
            f.runtime.backgroundPass()
            assertEquals(listOf("prepareBackground", "sessionGraphs", "session"), f.events)
        }

    @Test fun storedAccountIdIsNormalized() =
        fixture { f ->
            f.credentials.data = StoredSession("test", reminderAccount.uppercase(Locale.ROOT)).encode()
            assertEquals(reminderAccount, f.runtime.storedAccountId())
        }

    @Test fun wipeMarkerAndCorruptMarkerRejectTheSession() =
        fixture { f ->
            f.credentials.data = StoredSession("test", reminderAccount).encode()
            f.marker.write(AccountID.of(reminderAccount))
            assertNull(f.runtime.storedAccountId())
            f.marker.file.writeText("invalid")
            assertNull(f.runtime.storedAccountId())
            assertEquals(0, f.credentials.reads)
        }

    @Test fun corruptSessionIsNotDeleted() =
        fixture { f ->
            val bytes = "invalid".encodeToByteArray()
            f.credentials.data = bytes
            assertNull(f.runtime.storedAccountId())
            assertEquals(0, f.credentials.deletes)
            assertArrayEquals(bytes, f.credentials.data)
        }

    @Test fun unreadableWipeMarkerRejectsTheSessionWithoutReadingCredentials() =
        fixture { f ->
            f.credentials.data = StoredSession("test", reminderAccount).encode()
            check(f.marker.file.mkdir())
            assertNull(f.runtime.storedAccountId())
            assertEquals(0, f.credentials.reads)
            assertEquals(0, f.credentials.deletes)
        }

    @Test fun sessionIoFailureRejectsTheSession() = rejectsUnreadableSession(IOException())

    @Test fun sessionKeyStoreFailureRejectsTheSession() = rejectsUnreadableSession(KeyStoreException())

    @Test fun sessionSecurityFailureRejectsTheSession() = rejectsUnreadableSession(GeneralSecurityException())

    @Test fun sessionProviderFailureRejectsTheSession() = rejectsUnreadableSession(ProviderException())

    @Test fun sessionPermissionFailureRejectsTheSession() = rejectsUnreadableSession(SecurityException())

    @Test fun invalidSessionKeyRejectsTheSession() = rejectsUnreadableSession(PermanentSessionKeyFailure())

    @Test fun storedAccountReadPreservesCancellation() =
        fixture { f ->
            val cancelled = CancellationException()
            f.sessionReadFailure = cancelled
            try {
                f.runtime.storedAccountId()
                fail("account read swallowed cancellation")
            } catch (actual: CancellationException) {
                assertSame(cancelled, actual.cause ?: actual)
            }
        }

    private fun rejectsUnreadableSession(error: Throwable) =
        fixture { f ->
            val bytes = StoredSession("test", reminderAccount).encode()
            f.credentials.data = bytes
            f.sessionReadFailure = error
            assertNull(f.runtime.storedAccountId())
            assertEquals(0, f.credentials.deletes)
            assertArrayEquals(bytes, f.credentials.data)
        }

    private class Fixture(
        scope: TestScope,
        val root: File,
    ) {
        val events = mutableListOf<String>()
        val credentials = MemorySessionStore()
        var sessionReadFailure: Throwable? = null
        val attached = MutableStateFlow<String?>(null)
        var presence: SessionPresence = SessionPresence.SignedOut
        val dispatcher = StandardTestDispatcher(scope.testScheduler)
        val marker = WipeMarker(root, dispatcher)
        val deliveryGate = ReminderDeliveryGate()
        val runtime =
            AppReminderRuntime(
                dispatcher,
                scope.backgroundScope,
                0,
                { error("component is not read by runtime logic") },
                { reminderNow },
                object : SessionStore by credentials {
                    override suspend fun read(): ByteArray? {
                        sessionReadFailure?.let { throw it }
                        return credentials.read()
                    }
                },
                { marker },
                { events += "prepareBackground" },
                { events += "sessionGraphs" },
                {
                    events += "session"
                    presence
                },
                attached,
                { events += "reschedule" },
                { _, _ -> },
                {},
                deliveryGate,
            )
    }

    private fun fixture(test: suspend TestScope.(Fixture) -> Unit) =
        runTest(timeout = 60.seconds) {
            val root = TestRoots.temporaryStoreRoot()
            try {
                test(Fixture(this, root))
            } finally {
                withContext(NonCancellable) { check(root.deleteRecursively()) }
            }
        }
}
