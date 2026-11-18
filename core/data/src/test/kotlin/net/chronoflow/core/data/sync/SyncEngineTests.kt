package net.chronoflow.core.data.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.data.repository.AccountRepository
import net.chronoflow.core.data.repository.OutboxRepository
import net.chronoflow.core.data.repository.TripRepository
import net.chronoflow.core.data.sync.support.EngineFixture
import net.chronoflow.core.data.sync.support.SyncStore
import net.chronoflow.core.database.testing.exec
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.ProfileChange
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.model.session.SyncEpoch
import net.chronoflow.core.model.store.AccountRecord
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.core.sync.SyncEngine
import net.chronoflow.core.sync.SyncEvent
import net.chronoflow.core.sync.SyncResult
import net.chronoflow.core.sync.SyncScheduler
import net.chronoflow.core.sync.SyncState
import net.chronoflow.core.testing.sync.WriteBarrier
import net.chronoflow.core.testing.sync.check
import net.chronoflow.core.testing.sync.syncAccount
import net.chronoflow.core.testing.sync.syncDate
import net.chronoflow.core.testing.sync.syncOther
import net.chronoflow.core.testing.sync.syncTrip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SyncEngineTests {
    private fun engineTest(block: suspend TestScope.(EngineFixture) -> Unit) =
        runTest(timeout = 60.seconds) {
            val fixture = EngineFixture.make(this)
            try {
                block(fixture)
            } finally {
                fixture.close()
            }
        }

    private suspend fun cancelled(block: suspend () -> Any?) {
        try {
            block()
            error("expected cancellation")
        } catch (_: CancellationException) {
        }
    }

    // (new) A cancelled Android caller must keep its detail task visible until that task drains.
    @Test fun cancelAndWaitJoinsADetailFetchWhoseCallerWasCancelled() =
        engineTest { f ->
            f.engine.refreshNow()
            val gate = f.gate()
            f.source.configure(hook = { if (it == "detail") gate.block() })
            val request = async { f.engine.fetchDetail(syncTrip) }
            gate.waitUntilBlocked()
            request.cancel()
            runCurrent()
            val done = async { f.engine.cancelAndWait() }
            runCurrent()
            assertFalse("cancelAndWait returned while the detail task is still running", done.isCompleted)
            gate.release()
            runCurrent()
            assertTrue(done.isCompleted)
            assertTrue(request.isCompleted)
        }

    @Test fun concurrentRefreshesCoalesceOneRerun() =
        engineTest { f ->
            val gate = f.gate()
            f.source.configure(hook = { if (it == "identity") gate.block() })
            val first = async { f.engine.refreshNow() }
            gate.waitUntilBlocked()
            repeat(20) { f.engine.requestSync() }
            val second = async { f.engine.refreshNow() }
            runCurrent()
            f.source.configure()
            gate.release()
            assertEquals(SyncState.Idle, first.await().state)
            assertEquals(SyncState.Idle, second.await().state)
            assertTrue(f.source.calls.count { it == "identity" } in 2..3)
            assertEquals(1, f.source.maximumActiveCalls)
        }

    @Test fun cancellingOneWaiterPreservesSharedFlightCancelStarter() = cancellingOneWaiterPreservesSharedFlight(true)

    @Test fun cancellingOneWaiterPreservesSharedFlightCancelJoiner() = cancellingOneWaiterPreservesSharedFlight(false)

    private fun cancellingOneWaiterPreservesSharedFlight(cancelStarter: Boolean) =
        engineTest { f ->
            val gate = f.gate()
            f.source.configure(hook = { if (it == "identity") gate.block() })
            val starter = async { f.engine.refreshNow() }
            gate.waitUntilBlocked()
            val joiner = async { f.engine.refreshNow() }
            runCurrent()
            val canceled = if (cancelStarter) starter else joiner
            val remaining = if (cancelStarter) joiner else starter
            canceled.cancel()
            runCurrent()
            assertFalse(remaining.isCompleted)
            f.source.configure()
            gate.release()
            assertEquals(SyncState.Idle, remaining.await().state)
            cancelled { canceled.await() }
            assertEquals(2, f.source.calls.count { it == "identity" })
            assertEquals(1, f.source.maximumActiveCalls)
        }

    @Test fun queuedWriteDuringPullRunsAgainAndFlushes() =
        engineTest { f ->
            val gate = f.gate()
            f.source.configure(hook = { if (it == "trips") gate.block() })
            val run = async { f.engine.refreshNow() }
            gate.waitUntilBlocked()
            f.store.enqueue(OutboxChange.Walkthrough)
            f.engine.requestSync()
            f.source.configure()
            gate.release()
            assertEquals(0, run.await().pendingCount)
            assertEquals(1, f.source.calls.count { it == "send" })
        }

    @Test fun queuedCheckOffAfterDetailFailureRunsAgain() =
        engineTest { f ->
            val gate = f.gate()
            f.source.configure(hook = { method ->
                if (method == "detail") {
                    gate.block()
                    throw DataFailure.Server(500)
                }
            })
            val run = async { f.engine.refreshNow() }
            gate.waitUntilBlocked()
            f.store.enqueue(OutboxChange.CheckOff(check()))
            f.engine.requestSync()
            f.source.configure()
            gate.release()
            val result = run.await()
            assertEquals(SyncState.Idle, result.state)
            assertEquals(0, result.pendingCount)
            assertEquals(1.0, result.retryAfter)
            assertEquals(listOf(OutboxChange.CheckOff(check())), f.source.changes)
            assertEquals(
                listOf("identity", "account", "trips", "detail", "send", "identity", "account", "trips", "detail"),
                f.source.calls,
            )
        }

    @Test fun nonIdleFlightHonorsJoinedRefreshOffline() = nonIdleFlightHonorsJoinedRefresh(DataFailure.Offline)

    @Test fun nonIdleFlightHonorsJoinedRefreshServer500() = nonIdleFlightHonorsJoinedRefresh(DataFailure.Server(500))

    private fun nonIdleFlightHonorsJoinedRefresh(failure: DataFailure) =
        engineTest { f ->
            val gate = f.gate()
            f.source.configure(hook = { method ->
                if (method == "identity") {
                    gate.block()
                    throw failure
                }
            })
            val first = async { f.engine.refreshNow() }
            gate.waitUntilBlocked()
            val second = async { f.engine.refreshNow() }
            runCurrent()
            f.source.configure()
            gate.release()
            val result = first.await()
            assertEquals(SyncState.Idle, result.state)
            assertEquals(if (failure == DataFailure.Offline) null else 1.0, result.retryAfter)
            assertEquals(result, second.await())
            assertEquals(listOf("identity", "identity", "account", "trips", "detail"), f.source.calls)
        }

    @Test fun protectedFailureStopsCoalescedRerunUnauthorized() =
        protectedFailureStopsCoalescedRerun(DataFailure.Unauthorized(401))

    @Test fun protectedFailureStopsCoalescedRerunEmailUnverified() =
        protectedFailureStopsCoalescedRerun(DataFailure.EmailUnverified(403))

    private fun protectedFailureStopsCoalescedRerun(failure: DataFailure) =
        engineTest { f ->
            val gate = f.gate()
            f.source.configure(hook = { method ->
                if (method == "identity") {
                    gate.block()
                    throw failure
                }
            })
            val run = async { f.engine.refreshNow() }
            gate.waitUntilBlocked()
            f.engine.requestSync()
            gate.release()
            assertEquals(
                if (failure.status ==
                    401
                ) {
                    SyncState.Unauthorized
                } else {
                    SyncState.VerificationRequired
                },
                run.await().state,
            )
            assertEquals(listOf("identity"), f.source.calls)
        }

    @Test fun coalescedResultKeepsMaximumRetryAndRefusals900Then1() =
        coalescedResultKeepsMaximumRetryAndRefusals(
            900.0,
            1.0,
        )

    @Test fun coalescedResultKeepsMaximumRetryAndRefusals1Then900() =
        coalescedResultKeepsMaximumRetryAndRefusals(
            1.0,
            900.0,
        )

    private fun coalescedResultKeepsMaximumRetryAndRefusals(
        firstDelay: Double,
        secondDelay: Double,
    ) = engineTest { f ->
        val gate = f.gate()
        f.store.enqueue(OutboxChange.Profile(ProfileChange("Edited", "demo@example.com")))
        f.store.enqueue(
            OutboxChange.Settings(
                SettingsSection.Preferences,
                JsonObject(
                    mapOf("time_format" to JsonPrimitive("12")),
                ),
            ),
            1,
        )
        f.source.configure(
            failures = listOf(DataFailure.Server(422), DataFailure.Throttled(firstDelay, 429)),
            hook = { if (it == "identity") gate.block() },
        )
        val finished = mutableListOf<SyncResult>()
        backgroundScope.launch { f.engine.events().collect { if (it is SyncEvent.Finished) finished += it.result } }
        runCurrent()
        val run = async { f.engine.refreshNow() }
        gate.waitUntilBlocked()
        f.engine.requestSync()
        f.source.configure(failures = listOf(DataFailure.Throttled(secondDelay, 429)))
        gate.release()
        val result = run.await()
        assertEquals(900.0, result.retryAfter)
        assertEquals(1, result.pendingCount)
        assertEquals(1, result.refusals.size)
        assertEquals(3, f.source.changes.size)
        f.engine.cancelAndWait()
        runCurrent()
        assertEquals(listOf(result), finished)
    }

    @Test fun invalidationRejectsEveryLateResponseSend() = invalidationRejectsEveryLateResponse("send")

    @Test fun invalidationRejectsEveryLateResponseIdentity() = invalidationRejectsEveryLateResponse("identity")

    @Test fun invalidationRejectsEveryLateResponseAccount() = invalidationRejectsEveryLateResponse("account")

    @Test fun invalidationRejectsEveryLateResponseTrips() = invalidationRejectsEveryLateResponse("trips")

    @Test fun invalidationRejectsEveryLateResponseDetail() = invalidationRejectsEveryLateResponse("detail")

    private fun invalidationRejectsEveryLateResponse(method: String) =
        engineTest { f ->
            val gate = f.gate()
            if (method == "send") f.store.enqueue(OutboxChange.Walkthrough)
            f.source.configure(hook = { if (it == method) gate.block() })
            val events = mutableListOf<SyncEvent>()
            backgroundScope.launch { f.engine.events().collect { events += it } }
            runCurrent()
            val run = async { f.engine.refreshNow() }
            gate.waitUntilBlocked()
            f.store.epoch.invalidate()
            gate.release()
            cancelled { run.await() }
            assertEquals(method, f.source.calls.last())
            if (method == "send") assertEquals(1, OutboxRepository(f.store.database).pending().size)
            if (method == "identity") assertEquals("Local", AccountRepository(f.store.database).account()?.name)
            if (method == "account" ||
                method == "trips"
            ) {
                assertNull(AccountRepository(f.store.database).account()?.syncedAt)
            }
            if (method == "detail") assertNull(TripRepository(f.store.database).plan(syncTrip, "hash"))
            f.engine.cancelAndWait()
            runCurrent()
            assertEquals(1, events.size)
        }

    @Test fun callerCancellationDrainsLateResponseSend() = callerCancellationDrainsLateResponse("send")

    @Test fun callerCancellationDrainsLateResponseIdentity() = callerCancellationDrainsLateResponse("identity")

    @Test fun callerCancellationDrainsLateResponseAccount() = callerCancellationDrainsLateResponse("account")

    @Test fun callerCancellationDrainsLateResponseTrips() = callerCancellationDrainsLateResponse("trips")

    @Test fun callerCancellationDrainsLateResponseDetail() = callerCancellationDrainsLateResponse("detail")

    private fun callerCancellationDrainsLateResponse(method: String) =
        engineTest { f ->
            val gate = f.gate()
            if (method == "send") f.store.enqueue(OutboxChange.Walkthrough)
            f.source.configure(hook = { if (it == method) gate.block() })
            val run = async { f.engine.refreshNow() }
            gate.waitUntilBlocked()
            run.cancel()
            gate.release()
            cancelled { run.await() }
            runCurrent()
            assertEquals(method, f.source.calls.last())
            f.source.configure()
            assertEquals(SyncState.Idle, f.engine.refreshNow().state)
        }

    @Test fun tripPageDetailFetchIsCancelledBeforeItsLateResponseCanWriteFalse() =
        tripPageDetailFetchIsCancelledBeforeItsLateResponseCanWrite(false)

    @Test fun tripPageDetailFetchIsCancelledBeforeItsLateResponseCanWriteTrue() =
        tripPageDetailFetchIsCancelledBeforeItsLateResponseCanWrite(true)

    private fun tripPageDetailFetchIsCancelledBeforeItsLateResponseCanWrite(cancelRequest: Boolean) =
        engineTest { f ->
            val gate = f.gate()
            f.source.configure(hook = {
                if (it == "detail") {
                    val blocked = backgroundScope.async(start = CoroutineStart.UNDISPATCHED) { gate.block() }
                    try {
                        blocked.await()
                    } catch (_: CancellationException) {
                        gate.release()
                        withContext(NonCancellable) { blocked.await() }
                    }
                }
            })
            val request = async { f.engine.fetchDetail(syncTrip) }
            gate.waitUntilBlocked()
            if (cancelRequest) request.cancel()
            f.engine.cancelAndWait()
            cancelled { request.await() }
            assertNull(TripRepository(f.store.database).plan(syncTrip, "hash"))
            cancelled { f.engine.fetchDetail(syncTrip) }
        }

    @Test fun cancelAndWaitStopsWorkAndPreservesQueue() =
        engineTest { f ->
            val gate = f.gate()
            f.store.enqueue(OutboxChange.Walkthrough)
            f.source.configure(hook = { if (it == "send") gate.block() })
            val run = async { f.engine.refreshNow() }
            gate.waitUntilBlocked()
            val canceled = async { f.engine.cancelAndWait() }
            run.cancel()
            gate.release()
            canceled.await()
            cancelled { run.await() }
            assertEquals(1, OutboxRepository(f.store.database).pending().size)
            cancelled { f.engine.refreshNow() }
            assertEquals(listOf("send"), f.source.calls)
        }

    @Test fun wrongAccountAndPreCancelledRequestNeverCallSource() =
        engineTest { f ->
            val wrong =
                SyncEngine(
                    f.store.sync,
                    f.source,
                    SyncEpoch(AccountID.of(syncOther), 1),
                    scheduler = SyncScheduler({ syncDate }, f.scheduler::sleep),
                    dispatcher = f.dispatcher,
                )
            try {
                try {
                    wrong.refreshNow()
                    error("expected account mismatch")
                } catch (_: StoreFailure.InvalidAccountID) {
                }
                assertTrue(f.source.calls.isEmpty())
                val gate = f.gate()
                val run =
                    async {
                        gate.block()
                        f.engine.refreshNow()
                    }
                gate.waitUntilBlocked()
                run.cancel()
                gate.release()
                cancelled { run.await() }
                assertTrue(f.source.calls.isEmpty())
            } finally {
                wrong.cancelAndWait()
            }
        }

    @Test fun firstPullThenOfflineRestartAndAccountIsolation() =
        engineTest { f ->
            f.source.configure(trips = emptyList())
            f.store.database.write {
                it.exec("DELETE FROM trips")
                it.exec("DELETE FROM account")
            }
            assertEquals(SyncState.Idle, f.engine.refreshNow().state)
            val marker = AccountRepository(f.store.database).account()?.syncedAt
            assertNotNull(marker)
            f.engine.cancelAndWait()
            f.store.database.close()
            f.store.database.open(syncAccount)
            val offline =
                SyncEngine(
                    f.store.sync,
                    f.source,
                    f.store.epoch,
                    scheduler = SyncScheduler({ syncDate }, f.scheduler::sleep),
                    isOnline = { false },
                    dispatcher = f.dispatcher,
                )
            val other = SyncStore(f.dispatcher, syncOther)
            try {
                assertEquals(SyncState.Offline, offline.refreshNow().state)
                assertEquals(marker, AccountRepository(f.store.database).account()?.syncedAt)
                assertNull(AccountRepository(other.database).account()?.syncedAt)
                assertTrue(OutboxRepository(other.database).pending().isEmpty())
            } finally {
                offline.cancelAndWait()
                other.close()
            }
        }

    @Test fun sanitizedResultsRetainTransientWorkUnauthorized() =
        sanitizedResultsRetainTransientWork(DataFailure.Unauthorized(401))

    @Test fun sanitizedResultsRetainTransientWorkEmailUnverified() =
        sanitizedResultsRetainTransientWork(DataFailure.EmailUnverified(403))

    @Test fun sanitizedResultsRetainTransientWorkOffline() = sanitizedResultsRetainTransientWork(DataFailure.Offline)

    @Test fun sanitizedResultsRetainTransientWorkServer500() =
        sanitizedResultsRetainTransientWork(
            DataFailure.Server(500),
        )

    private fun sanitizedResultsRetainTransientWork(failure: DataFailure) =
        engineTest { f ->
            AccountRepository(f.store.database).upsert(AccountRecord(syncAccount, "Local", "demo@example.com", true))
            assertEquals(true, AccountRepository(f.store.database).account()?.hasVerifiedEmail)
            f.store.enqueue(
                OutboxChange.Settings(
                    SettingsSection.Preferences,
                    JsonObject(
                        mapOf("time_format" to JsonPrimitive("12")),
                    ),
                ),
            )
            f.source.configure(failures = listOf(failure))
            val result = f.engine.refreshNow()
            val state =
                when (failure) {
                    is DataFailure.Unauthorized -> SyncState.Unauthorized
                    is DataFailure.EmailUnverified -> SyncState.VerificationRequired
                    DataFailure.Offline -> SyncState.Offline
                    else -> SyncState.Idle
                }
            assertEquals(state, result.state)
            if (failure is DataFailure.EmailUnverified) {
                assertEquals(
                    false,
                    AccountRepository(f.store.database).account()?.hasVerifiedEmail,
                )
            }
            assertEquals(1, result.pendingCount)
            if (failure is DataFailure.Server) assertEquals(1.0, result.retryAfter)
        }

    @Test fun debounceWithInjectedSchedulerAndRefreshBypass() =
        engineTest { f ->
            f.engine.requestSync()
            f.scheduler.waitForSleep()
            assertTrue(f.source.calls.isEmpty())
            assertEquals(listOf(1.0), f.scheduler.requestedDelays)
            assertEquals(SyncState.Idle, f.engine.refreshNow().state)
            f.scheduler.advance()
            f.engine.cancelAndWait()
            assertEquals(1, f.source.calls.count { it == "identity" })
        }

    @Test fun cancelledEpochCannotEnterQueuedResponseTransaction() =
        runTest(timeout = 60.seconds) {
            val barrier = WriteBarrier(StandardTestDispatcher(testScheduler))
            val store = SyncStore(barrier)
            try {
                barrier.block()
                val response =
                    async {
                        store.database.write(
                            store.epoch,
                        ) { it.queries.accountQueries.setAccountName("late response", syncAccount) }
                    }
                runCurrent()
                assertTrue(barrier.pendingCount > 0)
                store.epoch.invalidate()
                barrier.release()
                cancelled { response.await() }
                assertEquals("Local", AccountRepository(store.database).account()?.name)
            } finally {
                barrier.release()
                store.close()
            }
        }

    @Test fun cancelDrainsDebounceWithoutFiring() =
        engineTest { f ->
            f.engine.requestSync()
            f.scheduler.waitForSleep()
            f.engine.cancelAndWait()
            assertEquals(0, f.scheduler.pendingCount)
            assertTrue(f.source.calls.isEmpty())
        }
}
