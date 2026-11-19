package net.chronoflow.core.data.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.data.repository.AccountRepository
import net.chronoflow.core.data.repository.OutboxRepository
import net.chronoflow.core.data.sync.support.TriggerFixture
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.session.StoredSession
import net.chronoflow.core.model.store.OutboxRecord
import net.chronoflow.core.sync.ActiveSyncDatabase
import net.chronoflow.core.sync.SessionSync
import net.chronoflow.core.sync.SyncDatabase
import net.chronoflow.core.sync.SyncLifecycle
import net.chronoflow.core.sync.SyncPathMonitoring
import net.chronoflow.core.sync.SyncState
import net.chronoflow.core.testing.sync.identity
import net.chronoflow.core.testing.sync.syncAccount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SyncTriggerTests {
    private fun triggerTest(block: suspend TestScope.(TriggerFixture) -> Unit) =
        runTest(timeout = 60.seconds) {
            val fixture = TriggerFixture.make(this)
            try {
                block(fixture)
            } finally {
                fixture.close()
            }
        }

    private fun sessionSync(f: TriggerFixture) =
        SessionSync(
            ActiveSyncDatabase { f.store.sync },
            f.scheduler,
            source = { f.source },
            unauthorized = { f.events.refused(it) },
            verification = { f.events.verify(it) },
            dispatcher = f.dispatcher,
        )

    private suspend fun cancelled(block: suspend () -> Any?) {
        try {
            block()
            error("expected cancellation")
        } catch (_: CancellationException) {
        }
    }

    @Test fun openOutboxReconnectAndSceneBurstsDebounceAndStopDrains() =
        triggerTest { f ->
            f.lifecycle.start()
            f.clock.waitForSleep()
            repeat(10) {
                val registrations = f.clock.requestedDelays.size
                f.lifecycle.requestSync()
                runCurrent()
                assertTrue(f.clock.requestedDelays.size > registrations)
            }
            val outboxRegistration = f.clock.requestedDelays.size
            f.store.enqueue(OutboxChange.Walkthrough)
            runCurrent()
            assertTrue(f.clock.requestedDelays.size > outboxRegistration)
            val registrations = f.clock.requestedDelays.size
            f.path.send(false)
            f.path.send(true)
            runCurrent()
            assertTrue(f.clock.requestedDelays.size > registrations)
            assertTrue(f.source.calls.isEmpty())
            f.clock.advance()
            runCurrent()
            assertEquals(
                SyncState.Idle,
                f.lifecycle.retryState.lastResult
                    ?.state,
            )
            assertEquals(listOf("send", "identity", "account", "trips", "detail"), f.source.calls)
            assertEquals(1, f.source.maximumActiveCalls)
            assertTrue(OutboxRepository(f.store.database).pending().isEmpty())
            f.lifecycle.stop()
            assertTrue(f.path.stopped)
            assertEquals(0, f.clock.pendingCount)
            cancelled { f.lifecycle.refreshNow() }
        }

    @Test fun cappedBackoffResetAndServerDeadline() =
        triggerTest { f ->
            f.source.configure(failMethod = "identity", methodFailure = DataFailure.Server(500))
            f.lifecycle.start()
            f.clock.waitForSleep()
            f.clock.advance()
            runCurrent()
            val sequence = listOf(1.0, 2.0, 4.0, 8.0, 16.0, 32.0, 64.0, 128.0, 256.0, 300.0, 300.0)
            sequence.forEachIndexed { index, delay ->
                assertEquals(delay, f.clock.requestedDelays.last(), 0.0)
                if (index == sequence.lastIndex) f.source.configure()
                f.clock.advance()
                runCurrent()
            }
            assertNotNull(AccountRepository(f.store.database).account()?.syncedAt)
            f.lifecycle.refreshNow()
            f.source.configure(failMethod = "identity", methodFailure = DataFailure.Server(500))
            f.lifecycle.refreshNow()
            runCurrent()
            assertEquals(1.0, f.clock.requestedDelays.last(), 0.0)
            f.source.configure(failMethod = "identity", methodFailure = DataFailure.Throttled(600.0, 429))
            f.clock.advance()
            runCurrent()
            assertEquals(600.0, f.clock.requestedDelays.last(), 0.0)
            val before = f.source.calls.size
            repeat(5) { f.lifecycle.requestSync() }
            f.lifecycle.refreshNow()
            assertEquals(before, f.source.calls.size)
            f.lifecycle.stop()
            assertEquals(0, f.clock.pendingCount)
        }

    @Test fun coalescedRecoveryPreservesServerDeadline() =
        triggerTest { f ->
            val gate = f.gate()
            f.store.enqueue(OutboxChange.Walkthrough)
            f.source.configure(
                failures = listOf(DataFailure.Throttled(900.0, 429)),
                hook = { if (it == "identity") gate.block() },
            )
            f.lifecycle.start()
            f.clock.waitForSleep()
            val run = async { f.lifecycle.refreshNow() }
            gate.waitUntilBlocked()
            f.lifecycle.requestSync()
            f.source.configure()
            gate.release()
            val result = run.await()
            assertEquals(0, result.pendingCount)
            assertEquals(900.0, result.retryAfter)
            runCurrent()
            assertEquals(900.0, f.clock.requestedDelays.last(), 0.0)
            val calls = f.source.calls.toList()
            assertEquals(result, f.lifecycle.refreshNow())
            assertEquals(calls, f.source.calls)
            assertEquals(1, f.clock.pendingCount)
            f.lifecycle.stop()
            assertEquals(0, f.clock.pendingCount)
        }

    @Test fun offlineWaitsForHintAndVerificationProbesIdentityOnly() =
        triggerTest { f ->
            f.source.configure(failMethod = "identity", methodFailure = DataFailure.Offline)
            f.lifecycle.start()
            f.clock.waitForSleep()
            assertEquals(listOf(1.0), f.clock.requestedDelays)
            assertEquals(SyncState.Offline, f.lifecycle.refreshNow().state)
            runCurrent()
            assertEquals(
                SyncState.Offline,
                f.lifecycle.retryState.lastResult
                    ?.state,
            )
            assertEquals(0, f.clock.pendingCount)
            assertEquals(listOf(1.0), f.clock.requestedDelays)
            f.source.configure(failMethod = "account", methodFailure = DataFailure.EmailUnverified(403))
            assertEquals(SyncState.VerificationRequired, f.lifecycle.refreshNow().state)
            f.source.configure(identity = identity(verified = false))
            f.store.enqueue(OutboxChange.Walkthrough)
            val before = f.source.calls.size
            assertEquals(SyncState.VerificationRequired, f.lifecycle.refreshNow().state)
            assertEquals(listOf("identity"), f.source.calls.drop(before))
            assertEquals(1, OutboxRepository(f.store.database).pending().size)
        }

    @Test fun unauthorizedDispatchesOnceAndInvalidEpochCannotDispatch() =
        triggerTest { f ->
            f.source.configure(failMethod = "identity", methodFailure = DataFailure.Unauthorized(401))
            f.lifecycle.start()
            assertEquals(SyncState.Unauthorized, f.lifecycle.refreshNow().state)
            runCurrent()
            f.lifecycle.requestSync()
            assertEquals(listOf(1L), f.events.unauthorized)
        }

    // (new) Android dispatch can run after stop invalidates an already accepted result's epoch.
    @Test fun acceptedUnauthorizedDispatchesAfterEpochInvalidation() =
        acceptedResultDispatchesAfterEpochInvalidation(DataFailure.Unauthorized(401), SyncState.Unauthorized)

    // (new) Verification has the same accepted-result dispatch boundary as unauthorized.
    @Test fun acceptedVerificationDispatchesAfterEpochInvalidation() =
        acceptedResultDispatchesAfterEpochInvalidation(DataFailure.EmailUnverified(403), SyncState.VerificationRequired)

    private fun acceptedResultDispatchesAfterEpochInvalidation(
        failure: DataFailure,
        expectedState: SyncState,
    ) = triggerTest { f ->
        lateinit var lifecycle: SyncLifecycle
        val dispatcher =
            object : CoroutineDispatcher() {
                // The delegate's test scheduler already runs every task serially.
                override fun limitedParallelism(
                    parallelism: Int,
                    name: String?,
                ): CoroutineDispatcher {
                    require(parallelism > 0)
                    return this
                }

                override fun dispatch(
                    context: CoroutineContext,
                    block: Runnable,
                ) {
                    f.dispatcher.dispatch(context) {
                        block.run()
                        if (lifecycle.retryState.lastResult?.state == expectedState) f.store.epoch.invalidate()
                    }
                }
            }
        lifecycle =
            SyncLifecycle(
                f.store.sync,
                f.source,
                f.store.epoch,
                f.scheduler,
                unauthorized = { f.events.refused(it) },
                verification = { f.events.verify(it) },
                dispatcher = dispatcher,
            )
        try {
            f.source.configure(failMethod = "identity", methodFailure = failure)
            lifecycle.start()
            f.clock.waitForSleep()
            f.clock.advance()
            runCurrent()
            assertEquals(expectedState, lifecycle.retryState.lastResult?.state)
            assertTrue(!f.store.epoch.isValid)
            val dispatched =
                when (expectedState) {
                    SyncState.Unauthorized -> f.events.unauthorized
                    else -> f.events.verification
                }
            assertEquals(listOf(1L), dispatched)
        } finally {
            lifecycle.stop()
        }
    }

    @Test fun invalidatedUnauthorizedResponseCannotDispatchWipe() =
        triggerTest { f ->
            val gate = f.gate()
            f.source.configure(
                failMethod = "identity",
                methodFailure = DataFailure.Unauthorized(401),
                hook = { if (it == "identity") gate.block() },
            )
            f.lifecycle.start()
            val refresh = async { f.lifecycle.refreshNow() }
            gate.waitUntilBlocked()
            f.store.epoch.invalidate()
            gate.release()
            cancelled { refresh.await() }
            f.lifecycle.stop()
            assertTrue(f.events.unauthorized.isEmpty())
        }

    // (new) Cancellation while starting must detach the stopped lifecycle so the same epoch can resume.
    @Test fun cancelledStartDetachesTheStoppedLifecycle() =
        triggerTest { f ->
            val gate = CompletableDeferred<Unit>()
            var gated = true
            val database =
                object : SyncDatabase by f.store.sync {
                    override suspend fun pending(): List<OutboxRecord> {
                        if (gated) {
                            gated = false
                            gate.await()
                        }
                        return f.store.sync.pending()
                    }
                }
            val sync =
                SessionSync(
                    ActiveSyncDatabase { database },
                    f.scheduler,
                    source = { f.source },
                    unauthorized = {},
                    verification = {},
                    dispatcher = f.dispatcher,
                )
            val session = StoredSession("test", syncAccount)
            try {
                val starter = backgroundScope.launch { sync.start(session, 5) }
                runCurrent()
                starter.cancel()
                runCurrent()
                assertNull(sync.refreshNow())
                sync.resume(session, 5)
                runCurrent()
                assertEquals(SyncState.Idle, sync.refreshNow()?.state)
            } finally {
                sync.stop()
            }
        }

    // (new) Android caller cancellation must not interrupt draining the replaced lifecycle.
    @Test fun cancelledReplacementStillStopsTheOldLifecycle() =
        triggerTest { f ->
            var cancelCaller: () -> Unit = {}
            val sync =
                SessionSync(
                    ActiveSyncDatabase { f.store.sync },
                    f.scheduler,
                    source = { f.source },
                    isCurrent = { _, epoch ->
                        if (epoch == 6L) cancelCaller()
                        true
                    },
                    unauthorized = { f.events.refused(it) },
                    verification = { f.events.verify(it) },
                    dispatcher = f.dispatcher,
                )
            val session = StoredSession("test", syncAccount)
            try {
                f.source.configure(failMethod = "identity", methodFailure = DataFailure.Server(500))
                sync.start(session, 5)
                sync.refreshNow()
                runCurrent()
                assertTrue(f.clock.pendingCount > 0)
                val replace = async { sync.start(session, 6) }
                cancelCaller = { replace.cancel() }
                runCurrent()
                assertTrue(replace.isCancelled)
                sync.stop()
                runCurrent()
                assertEquals("epoch-5 lifecycle still has a pending timer", 0, f.clock.pendingCount)
            } finally {
                sync.stop()
            }
        }

    @Test fun sessionPausePreservesServerDeadlineAndReplacementEpochDropsIt() =
        triggerTest { f ->
            f.source.configure(failMethod = "identity", methodFailure = DataFailure.Throttled(300.0, 429))
            val sync = sessionSync(f)
            val session = StoredSession("test", syncAccount)
            try {
                sync.start(session, 5)
                assertEquals(SyncState.Failed, sync.refreshNow()?.state)
                runCurrent()
                assertEquals(300.0, f.clock.requestedDelays.last(), 0.0)
                sync.setEnabled(false)
                assertEquals(0, f.clock.pendingCount)
                f.time.advance(10.0)
                val registrations = f.clock.requestedDelays.size
                sync.setEnabled(true)
                sync.resume(session, 5)
                runCurrent()
                assertTrue(f.clock.requestedDelays.size > registrations)
                assertEquals(290.0, f.clock.requestedDelays.last(), 0.0)
                assertEquals(listOf("identity"), f.source.calls)
                assertEquals(SyncState.Failed, sync.refreshNow()?.state)
                assertEquals(listOf("identity"), f.source.calls)
                sync.setEnabled(false)
                val replacement = f.clock.requestedDelays.size
                sync.setEnabled(true)
                sync.resume(session, 6)
                runCurrent()
                assertTrue(f.clock.requestedDelays.size > replacement)
                assertEquals(1.0, f.clock.requestedDelays.last(), 0.0)
                f.time.advance(1.0)
                f.source.configure(trips = emptyList())
                f.clock.advance()
                runCurrent()
                assertEquals(listOf("identity", "identity", "account", "trips"), f.source.calls)
                sync.setEnabled(false)
                sync.setEnabled(true)
                sync.resume(session, 7)
                runCurrent()
                assertEquals(1, f.clock.pendingCount)
                assertEquals(1.0, f.clock.requestedDelays.last(), 0.0)
            } finally {
                sync.stop()
            }
        }

    // (new) Android lifecycle owners can disable sync from a cancelled finally block.
    @Test fun disablingFromCancelledOwnerDrainsAndKeepsSyncDisabled() =
        triggerTest { f ->
            f.source.configure(failMethod = "identity", methodFailure = DataFailure.Server(500))
            val sync = sessionSync(f)
            val session = StoredSession("test", syncAccount)
            try {
                sync.start(session, 5)
                sync.refreshNow()
                runCurrent()
                assertTrue(f.clock.pendingCount > 0)
                val owner =
                    backgroundScope.launch {
                        try {
                            awaitCancellation()
                        } finally {
                            sync.setEnabled(false)
                        }
                    }
                runCurrent()
                owner.cancelAndJoin()
                runCurrent()
                assertEquals(0, f.clock.pendingCount)
                assertNull(sync.refreshNow())
                val calls = f.source.calls.toList()
                sync.resume(session, 5)
                runCurrent()
                assertEquals(calls, f.source.calls)
                assertEquals(0, f.clock.pendingCount)
                assertNull(sync.refreshNow())
            } finally {
                sync.stop()
            }
        }

    @Test fun sessionPausePreservesFailureExponent() =
        triggerTest { f ->
            f.source.configure(failMethod = "identity", methodFailure = DataFailure.Server(500))
            val sync = sessionSync(f)
            val session = StoredSession("test", syncAccount)
            try {
                sync.start(session, 5)
                sync.refreshNow()
                runCurrent()
                assertEquals(1.0, f.clock.requestedDelays.last(), 0.0)
                sync.setEnabled(false)
                f.time.advance(2.0)
                sync.setEnabled(true)
                sync.resume(session, 5)
                sync.refreshNow()
                runCurrent()
                assertEquals(2.0, f.clock.requestedDelays.last(), 0.0)
                assertEquals(listOf("identity", "identity"), f.source.calls)
            } finally {
                sync.stop()
            }
        }

    @Test fun restartSeedsBackoffFromPersistedOutboxAttempts() =
        triggerTest { f ->
            val row = f.store.enqueue(OutboxChange.Walkthrough)
            OutboxRepository(f.store.database).upsert(row.copy(attempts = 8))
            f.source.configure(failMethod = "identity", methodFailure = DataFailure.Server(500))
            f.lifecycle.start()
            f.lifecycle.refreshNow()
            runCurrent()
            assertEquals(256.0, f.clock.requestedDelays.last(), 0.0)
        }

    @Test fun newTriggerPreemptsExponentialRetryActivation() = newTriggerPreemptsExponentialRetry("activation")

    @Test fun newTriggerPreemptsExponentialRetryOutbox() = newTriggerPreemptsExponentialRetry("outbox")

    @Test fun newTriggerPreemptsExponentialRetryReconnect() = newTriggerPreemptsExponentialRetry("reconnect")

    private fun newTriggerPreemptsExponentialRetry(trigger: String) =
        triggerTest { f ->
            f.source.configure(failMethod = "identity", methodFailure = DataFailure.Server(500))
            f.lifecycle.start()
            listOf(1.0, 2.0, 4.0, 8.0, 16.0, 32.0, 64.0, 128.0).forEach { delay ->
                f.lifecycle.refreshNow()
                runCurrent()
                assertEquals(delay, f.clock.requestedDelays.last(), 0.0)
            }
            val before = f.source.calls.size
            val registrations = f.clock.requestedDelays.size
            when (trigger) {
                "outbox" -> {
                    f.store.enqueue(OutboxChange.Walkthrough)
                }

                "reconnect" -> {
                    f.path.send(false)
                    f.path.send(true)
                }

                else -> {
                    f.lifecycle.requestSync()
                }
            }
            runCurrent()
            assertTrue(f.clock.requestedDelays.size > registrations)
            assertEquals(1.0, f.clock.requestedDelays.last(), 0.0)
            assertEquals(before, f.source.calls.size)
            f.clock.advance()
            runCurrent()
            val expected = if (trigger == "outbox") listOf("send", "identity") else listOf("identity")
            assertEquals(expected, f.source.calls.drop(before))
            assertEquals(256.0, f.clock.requestedDelays.last(), 0.0)
        }

    @Test fun outboxBackoffCanBePreemptedButServerDeadlineCannot() =
        triggerTest { f ->
            f.store.enqueue(OutboxChange.Walkthrough)
            f.source.configure(trips = emptyList(), failures = listOf(DataFailure.Throttled(300.0, 429)))
            f.lifecycle.start()
            val result = f.lifecycle.refreshNow()
            assertEquals(300.0, result.serverRetryAfter)
            runCurrent()
            assertEquals(300.0, f.clock.requestedDelays.last(), 0.0)
            val before = f.source.calls.toList()
            val registrations = f.clock.requestedDelays.size
            f.lifecycle.requestSync()
            f.lifecycle.refreshNow()
            assertEquals(before, f.source.calls)
            assertEquals(registrations, f.clock.requestedDelays.size)
        }

    // (new) Android callback registration can fail without disabling explicit sync or escaping the scope.
    @Test fun throwingPathMonitorDoesNotEscapeTheLifecycle() =
        triggerTest { f ->
            var pathStopped = false
            val path =
                object : SyncPathMonitoring {
                    override fun updates(): Flow<Boolean> = flow { throw IllegalStateException("too many callbacks") }

                    override suspend fun stop() {
                        pathStopped = true
                    }
                }
            val lifecycle =
                SyncLifecycle(
                    f.store.sync,
                    f.source,
                    f.store.epoch,
                    f.scheduler,
                    path = path,
                    dispatcher = f.dispatcher,
                )
            try {
                lifecycle.start()
                runCurrent()
                assertEquals(SyncState.Idle, lifecycle.refreshNow().state)
            } finally {
                lifecycle.stop()
            }
            assertTrue(pathStopped)
            assertEquals(0, f.clock.pendingCount)
        }

    @Test fun reconnectAloneStartsOneDebouncedCycle() =
        triggerTest { f ->
            f.lifecycle.start()
            f.clock.waitForSleep()
            f.clock.advance()
            runCurrent()
            assertEquals(
                SyncState.Idle,
                f.lifecycle.retryState.lastResult
                    ?.state,
            )
            assertEquals(listOf("identity", "account", "trips", "detail"), f.source.calls)
            val registrations = f.clock.requestedDelays.size
            f.path.send(false)
            f.path.send(true)
            runCurrent()
            assertTrue(f.clock.requestedDelays.size > registrations)
            assertEquals(1.0, f.clock.requestedDelays.last(), 0.0)
            assertEquals(listOf("identity", "account", "trips", "detail"), f.source.calls)
            f.clock.advance()
            runCurrent()
            f.lifecycle.stop()
            val expected = listOf("identity", "account", "trips", "detail", "identity", "account", "trips")
            assertEquals(expected, f.source.calls)
            assertEquals(1, f.source.maximumActiveCalls)
        }
}
