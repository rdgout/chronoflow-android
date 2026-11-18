@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.core.sync.session

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.api.SessionResult
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.model.session.SignOutResult
import net.chronoflow.core.testing.sync.TestGate
import net.chronoflow.core.testing.sync.eventually
import net.chronoflow.core.testing.sync.syncAccount
import net.chronoflow.core.testing.sync.syncOther
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

class SignOutTests {
    private class Harness(
        scope: TestScope,
        hooks: (SessionEventLog) -> SessionTeardown = { SessionTeardown(sleep = { awaitCancellation() }) },
    ) {
        val log = SessionEventLog()
        val credentials = MemorySessionStore(log = log)
        val accounts = RecordingAccountStore(log)
        val controller =
            SessionController(credentials, accounts, hooks(log), StandardTestDispatcher(scope.testScheduler))
    }

    @Test fun offlinePendingReturnsConfirmationAndPreservesCredentials() =
        runTest(timeout = 60.seconds) {
            var flushes = 0
            val h =
                Harness(this) { log ->
                    SessionTeardown(
                        sleep = { awaitCancellation() },
                        pendingCount = { 2 },
                        flush = {
                            flushes++
                            throw IOException()
                        },
                        logout = { log.record("logout") },
                        beginWipe = { log.record("marker") },
                    )
                }
            h.controller.accept(sessionResult())
            assertEquals(SignOutResult.NeedsConfirmation(2), h.controller.signOut(false))
            assertEquals(1, flushes)
            assertNotNull(h.credentials.data)
            assertEquals(SessionPresence.SignedIn(syncAccount), h.controller.presence)
            assertFalse(h.log.events.contains("marker"))
            assertFalse(h.log.events.contains("logout"))
            h.controller.close()
        }

    private fun discardOrder(forced: Boolean) =
        runTest(timeout = 60.seconds) {
            val gate = TestGate()
            val h =
                Harness(this) { log ->
                    SessionTeardown(
                        sleep = { awaitCancellation() },
                        flush = { log.record("flush") },
                        beginWipe = {
                            log.record("marker")
                            gate.block()
                        },
                        logout = {
                            assertEquals("test-token", it.token)
                            log.record("logout")
                        },
                        detach = { log.record("detach") },
                        erase = { log.record("erase") },
                        finishWipe = { log.record("finish") },
                    )
                }
            h.controller.attach(stopSync = { h.log.record("drain") })
            h.controller.accept(sessionResult())
            val first = backgroundScope.async { h.controller.signOut(true, forced) }
            gate.waitUntilBlocked()
            val second = backgroundScope.async { h.controller.signOut(true, forced) }
            runCurrent()
            gate.release()
            assertEquals(SignOutResult.SignedOut, first.await())
            assertEquals(SignOutResult.SignedOut, second.await())
            val events = h.log.events
            assertEquals(1, events.count { it == "marker" })
            assertFalse(events.contains("flush"))
            assertEquals(!forced, events.contains("logout"))
            val marker = events.indexOf("marker")
            val drain = events.indexOfFirstAfter("drain", marker)
            val detach = events.indexOf("detach")
            val close = events.indexOfFirstAfter("close", detach)
            val erase = events.indexOf("erase")
            val delete = events.indexOf("delete")
            assertTrue(
                marker >= 0 && marker < drain && drain < detach && detach < close && close < erase && erase < delete,
            )
            assertTrue(events.indexOf("finish") > delete)
            assertNull(h.credentials.data)
            assertEquals(SessionPresence.SignedOut, h.controller.presence)
            h.controller.close()
        }

    @Test fun discardAndForcedOrderAndConcurrentRequestsDiscard() = discardOrder(false)

    @Test fun discardAndForcedOrderAndConcurrentRequestsForced() = discardOrder(true)

    @Test fun oldUnauthorizedCannotWipeReplacementAndCurrentUnauthorizedWipesOnce() =
        runTest(timeout = 60.seconds) {
            val h =
                Harness(this) { log ->
                    SessionTeardown(sleep = {
                        awaitCancellation()
                    }, beginWipe = { log.record("marker") }, logout = { log.record("logout") })
                }
            h.controller.accept(sessionResult())
            val old = h.controller.epoch
            h.controller.accept(sessionResult(syncOther))
            h.controller.unauthorized(old)
            assertEquals(syncOther, h.controller.storedSession?.accountId)
            assertFalse(h.log.events.contains("marker"))
            val current = h.controller.epoch
            h.controller.unauthorized(current)
            h.controller.unauthorized(current)
            assertEquals(1, h.log.events.count { it == "marker" })
            assertFalse(h.log.events.contains("logout"))
            assertNull(h.credentials.data)
            h.controller.close()
        }

    @Test fun failedWipeRetriesBeforeAnotherAccountCanOpen() =
        runTest(timeout = 60.seconds) {
            var pendingWipe = false
            val h =
                Harness(this) {
                    SessionTeardown(
                        sleep = { awaitCancellation() },
                        prepare = { if (pendingWipe) throw IOException() },
                        beginWipe = { pendingWipe = true },
                        erase = { throw IOException() },
                    )
                }
            h.controller.accept(sessionResult())
            ioFailure { h.controller.signOut(true, true) }
            assertNull(h.credentials.data)
            ioFailure { h.controller.accept(sessionResult(syncOther)) }
            assertFalse(h.log.events.contains("open:$syncOther"))
            h.controller.close()
        }

    @Test fun logoutDeadlineCancelsRequestAndStillErases() =
        runTest(timeout = 60.seconds) {
            val requestEntered = CompletableDeferred<Unit>()
            var cancelled = false
            val deadline = TestGate()
            val h =
                Harness(this) { log ->
                    SessionTeardown(
                        sleep = {
                            assertEquals(3.seconds, it)
                            deadline.block()
                        },
                        logout = {
                            requestEntered.complete(Unit)
                            try {
                                awaitCancellation()
                            } finally {
                                cancelled = true
                            }
                        },
                        erase = { log.record("erase") },
                    )
                }
            h.controller.accept(sessionResult())
            val task = backgroundScope.async { h.controller.signOut(true) }
            requestEntered.await()
            deadline.waitUntilBlocked()
            deadline.release()
            assertEquals(SignOutResult.SignedOut, task.await())
            assertTrue(cancelled)
            assertTrue(h.log.events.contains("erase"))
            assertNull(h.credentials.data)
            h.controller.close()
        }

    @Test fun activationCannotCaptureCredentialsDuringReplacement() =
        runTest(timeout = 60.seconds) {
            val h = Harness(this)
            h.controller.accept(sessionResult())
            val old = h.controller.activeSession()!!
            val write = TestGate()
            h.credentials.writeGate = write
            val replacement =
                backgroundScope.async {
                    h.controller.accept(SessionResult("replacement-token", sessionResult().account))
                }
            write.waitUntilBlocked()
            assertNull(h.controller.activeSession())
            write.release()
            replacement.await()
            val current = h.controller.activeSession()!!
            assertTrue(current.epoch > old.epoch)
            assertEquals("replacement-token", current.session.token)
            h.controller.unauthorized(old.epoch)
            assertEquals(current, h.controller.activeSession())
            h.controller.close()
        }

    @Test fun emptyOutboxSkipsFlushBeforeLogoutAndWipe() =
        runTest(timeout = 60.seconds) {
            val h =
                Harness(this) { log ->
                    SessionTeardown(
                        sleep = { awaitCancellation() },
                        pendingCount = {
                            log.record("pending")
                            0
                        },
                        flush = { log.record("flush") },
                        beginWipe = { log.record("marker") },
                        logout = { log.record("logout") },
                    )
                }
            h.controller.accept(sessionResult())
            assertEquals(SignOutResult.SignedOut, h.controller.signOut(false))
            assertFalse(h.log.events.contains("flush"))
            assertEquals(1, h.log.events.count { it == "pending" })
            assertTrue(h.log.events.contains("marker"))
            assertTrue(h.log.events.contains("logout"))
            assertNull(h.credentials.data)
            h.controller.close()
        }

    private suspend fun TestScope.queuedFlight(discard: Boolean) {
        val h =
            Harness(this) { log ->
                SessionTeardown(
                    sleep = { awaitCancellation() },
                    pendingCount = { 2 },
                    flush = { throw IOException() },
                    beginWipe = { log.record("marker") },
                )
            }
        h.controller.accept(sessionResult())
        val oldEpoch = h.controller.epoch
        val write = TestGate()
        h.credentials.writeGate = write
        val replacement =
            backgroundScope.async {
                h.controller.accept(SessionResult("replacement-token", sessionResult().account))
            }
        write.waitUntilBlocked()
        val forced = backgroundScope.async { h.controller.unauthorized(oldEpoch) }
        assertTrue(eventually { h.controller.signOutFlight != null })
        val generation = h.controller.generation
        val explicit = backgroundScope.async { h.controller.signOut(discard) }
        val upgraded = eventually { h.controller.generation > generation }
        write.release()
        assertTrue(upgraded)
        replacement.await()
        assertEquals(if (discard) SignOutResult.SignedOut else SignOutResult.NeedsConfirmation(2), explicit.await())
        forced.await()
        if (discard) {
            assertEquals(SessionPresence.SignedOut, h.controller.presence)
            assertNull(h.controller.activeSession())
            assertNull(h.credentials.data)
        } else {
            assertEquals(SessionPresence.SignedIn(syncAccount), h.controller.presence)
            assertEquals("replacement-token", h.controller.storedSession?.token)
            assertNotNull(h.credentials.data)
            assertFalse(h.log.events.contains("marker"))
        }
        h.controller.close()
    }

    @Test fun explicitSignOutUpgradesQueuedEpochScopedFlight() = runTest(timeout = 60.seconds) { queuedFlight(true) }

    @Test fun staleForcedFlightPreservesReplacementPendingRowsWithoutExplicitDiscard() =
        runTest(timeout = 60.seconds) {
            queuedFlight(false)
        }

    @Test fun failedForcedMarkerIsRetriedOnActivation() =
        runTest(timeout = 60.seconds) {
            var attempts = 0
            val h =
                Harness(this) { log ->
                    SessionTeardown(
                        sleep = { awaitCancellation() },
                        beginWipe = {
                            attempts++
                            if (attempts == 1) throw IOException()
                        },
                        flush = { log.record("flush") },
                        logout = { log.record("logout") },
                    )
                }
            h.controller.accept(sessionResult())
            val epoch = h.controller.epoch
            ioFailure { h.controller.unauthorized(epoch) }
            assertEquals(epoch, h.controller.pendingUnauthorizedEpoch)
            h.controller.retryUnauthorized()
            assertEquals(2, attempts)
            assertEquals(SessionPresence.SignedOut, h.controller.presence)
            assertNull(h.controller.pendingUnauthorizedEpoch)
            assertNull(h.credentials.data)
            assertFalse(h.log.events.contains("flush"))
            assertFalse(h.log.events.contains("logout"))
            h.controller.retryUnauthorized()
            assertEquals(2, attempts)
            h.controller.close()
        }

    /** Cleanup belongs to the session, so cancelling an Android screen cannot abandon it. */
    @Test fun cancelingTheCallerDoesNotCancelCleanup() =
        runTest(timeout = 60.seconds) {
            val marker = TestGate()
            var finished = false
            val h =
                Harness(this) {
                    SessionTeardown(sleep = { awaitCancellation() }, beginWipe = { marker.block() }, finishWipe = {
                        finished =
                            true
                    })
                }
            h.controller.accept(sessionResult())
            val caller = backgroundScope.async { h.controller.signOut(true) }
            marker.waitUntilBlocked()
            caller.cancel()
            runCurrent()
            marker.release()
            runCurrent()
            assertTrue(finished)
            assertNull(h.credentials.data)
            assertEquals(SessionPresence.SignedOut, h.controller.presence)
            h.controller.close()
        }
}

private fun List<String>.indexOfFirstAfter(
    value: String,
    start: Int,
): Int = withIndex().firstOrNull { it.index > start && it.value == value }?.index ?: -1

private suspend fun ioFailure(body: suspend () -> Unit) {
    try {
        body()
        fail("expected storage failure")
    } catch (_: IOException) {
    }
}
