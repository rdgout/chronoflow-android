@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.core.sync.session

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.model.session.SignOutResult
import net.chronoflow.core.testing.sync.TestGate
import net.chronoflow.core.testing.sync.syncAccount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/** Android-only regression for separate sync-job cancellation during owned teardown. */
class CancelledFlushSignOutTest {
    private class Fixture(
        scope: TestScope,
        flush: suspend () -> Unit,
    ) {
        val log = SessionEventLog()
        val credentials = MemorySessionStore(log = log)
        val accounts = RecordingAccountStore(log)
        var pendingReads = 0
        val controller =
            SessionController(
                credentials,
                accounts,
                SessionTeardown(
                    sleep = { awaitCancellation() },
                    pendingCount = {
                        pendingReads++
                        2
                    },
                    flush = flush,
                    beginWipe = { log.record("marker") },
                    logout = { log.record("logout") },
                    detach = { log.record("detach") },
                    erase = { log.record("erase") },
                    finishWipe = { log.record("finish") },
                ),
                StandardTestDispatcher(scope.testScheduler),
            )
    }

    @Test fun cancelledFlushRecountsAndPreservesPendingSession() =
        runTest(timeout = 60.seconds) {
            var flushes = 0
            val f =
                Fixture(this) {
                    flushes++
                    throw CancellationException("sync stopped")
                }
            try {
                f.controller.accept(sessionResult())
                assertEquals(SignOutResult.NeedsConfirmation(2), f.controller.signOut(false))
                assertEquals(1, flushes)
                assertEquals(2, f.pendingReads)
                assertEquals(SessionPresence.SignedIn(syncAccount), f.controller.presence)
                assertNotNull(f.credentials.data)
                assertEquals(syncAccount, f.accounts.accountID?.rawValue)
                assertFalse(f.log.events.contains("marker"))
                assertFalse(f.log.events.contains("logout"))
            } finally {
                f.controller.close()
            }
        }

    @Test fun unauthorizedMergedDuringCancelledFlushStillWipesForBothCallers() =
        runTest(timeout = 60.seconds) {
            val flush = TestGate()
            val f =
                Fixture(this) {
                    flush.block()
                    throw CancellationException("sync stopped")
                }
            try {
                f.controller.accept(sessionResult())
                val epoch = f.controller.epoch
                val voluntary = backgroundScope.async { f.controller.signOut(false) }
                flush.waitUntilBlocked()
                val forced = backgroundScope.async { f.controller.unauthorized(epoch) }
                runCurrent()
                assertEquals(epoch, f.controller.pendingUnauthorizedEpoch)
                flush.release()
                assertEquals(SignOutResult.SignedOut, voluntary.await())
                assertEquals(SignOutResult.SignedOut, forced.await())
                assertEquals(2, f.pendingReads)
                assertEquals(SessionPresence.SignedOut, f.controller.presence)
                assertNull(f.credentials.data)
                assertNull(f.accounts.accountID)
                assertNull(f.controller.pendingUnauthorizedEpoch)
                assertFalse(f.log.events.contains("logout"))
                assertEquals(listOf("marker", "detach", "close", "erase", "delete", "finish"), f.log.events.takeLast(6))
            } finally {
                flush.release()
                f.controller.close()
            }
        }
}
