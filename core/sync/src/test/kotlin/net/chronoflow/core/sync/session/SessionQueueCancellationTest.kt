@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.core.sync.session

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.model.session.SignOutResult
import net.chronoflow.core.model.session.StoredSession
import net.chronoflow.core.testing.sync.TestGate
import net.chronoflow.core.testing.sync.syncAccount
import net.chronoflow.core.testing.sync.syncOther
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/** Android-only regression for cancelled Kotlin waiters preserving the session queue. */
class SessionQueueCancellationTest {
    @Test fun cancelledRestoreCannotLetReplacementOvertakeSignOut() =
        runTest(timeout = 60.seconds) {
            val log = SessionEventLog()
            val credentials = MemorySessionStore(log = log)
            val accounts = RecordingAccountStore(log)
            val logout = TestGate()
            val controller =
                SessionController(
                    credentials,
                    accounts,
                    SessionTeardown(
                        sleep = { awaitCancellation() },
                        logout = {
                            log.record("logout")
                            logout.block()
                        },
                        detach = { log.record("detach") },
                    ),
                    StandardTestDispatcher(testScheduler),
                )
            try {
                controller.accept(sessionResult(syncAccount))
                log.events.clear()
                val signOut = backgroundScope.async { controller.signOut(true) }
                logout.waitUntilBlocked()
                val restore = backgroundScope.async { controller.restore() }
                runCurrent()
                restore.cancelAndJoin()
                val replacement = backgroundScope.async { controller.accept(sessionResult(syncOther)) }
                runCurrent()
                logout.release()
                assertEquals(SignOutResult.SignedOut, signOut.await())
                replacement.await()
                assertEquals(syncOther, accounts.accountID?.rawValue)
                assertEquals(syncOther, StoredSession.parse(checkNotNull(credentials.data))?.accountId)
                assertEquals(SessionPresence.SignedIn(syncOther), controller.presence)
                assertEquals(
                    listOf("logout", "detach", "close", "delete", "open:$syncOther", "upsert:$syncOther", "write"),
                    log.events,
                )
            } finally {
                logout.release()
                controller.close()
            }
        }
}
