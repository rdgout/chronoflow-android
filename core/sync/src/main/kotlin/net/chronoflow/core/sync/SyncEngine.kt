package net.chronoflow.core.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.api.AccountSecurityChange
import net.chronoflow.core.model.api.AccountSecurityResult
import net.chronoflow.core.model.session.SyncEpoch
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
class SyncEngine(
    private val database: SyncDatabase,
    source: DataSource,
    private val epoch: SyncEpoch,
    val retention: PlanRetention = PlanRetention(),
    private val scheduler: SyncScheduler,
    private val isOnline: suspend () -> Boolean = { true },
    dispatcher: CoroutineDispatcher,
) {
    private val confined = dispatcher.limitedParallelism(1)
    private val scope = CoroutineScope(SupervisorJob() + confined)
    private var source: DataSource? = source
    private var flight: Deferred<SyncResult>? = null
    private var flightEpoch: SyncEpoch? = null
    private var flightWaiters: RefreshWaiters? = null
    private var timer: Job? = null
    private val canceledTimers = mutableMapOf<Long, Job>()
    private var timerID = 0L
    private var rerun = false
    private var stopped = false
    private val detailTasks = mutableMapOf<UUID, Pair<SyncEpoch, Deferred<Unit>>>()
    private val observers = mutableMapOf<UUID, Channel<SyncEvent>>()

    fun events(): Flow<SyncEvent> =
        flow {
            val id = UUID.randomUUID()
            val channel = Channel<SyncEvent>(32, BufferOverflow.DROP_OLDEST)
            withContext(confined) {
                if (stopped) channel.close() else observers[id] = channel
            }
            try {
                emitAll(channel.receiveAsFlow())
            } finally {
                withContext(NonCancellable + confined) {
                    observers.remove(id)
                    channel.close()
                }
            }
        }

    suspend fun deleteAccount(password: String) =
        withContext(confined) {
            epoch.check()
            val source = source
            if (stopped || source == null) throw CancellationException()
            source.deleteAccount(password)
            epoch.check()
        }

    suspend fun accountSecurity(change: AccountSecurityChange): AccountSecurityResult =
        withContext(confined) {
            epoch.check()
            val source = source
            if (stopped || source == null) throw CancellationException()
            val result = source.accountSecurity(change)
            currentCoroutineContext().ensureActive()
            epoch.check()
            if (stopped) throw CancellationException()
            result
        }

    suspend fun fetchDetail(tripId: String) =
        withContext(confined) {
            epoch.check()
            val source = source
            if (stopped || source == null) throw CancellationException()
            val id = UUID.randomUUID()
            val requestEpoch = epoch.child()
            val pull = FullPull(database, source, retention, scheduler.now)
            val task = scope.async { pull.fetchDetail(tripId, requestEpoch) }
            detailTasks[id] = requestEpoch to task
            try {
                awaitOwned(task) {
                    requestEpoch.invalidate()
                    task.cancel()
                }
                currentCoroutineContext().ensureActive()
                requestEpoch.check()
            } finally {
                detailTasks.remove(id)
            }
        }

    suspend fun requestSync() =
        withContext(confined) {
            if (stopped) return@withContext
            if (flight != null) {
                rerun = true
                return@withContext
            }
            cancelTimer()
            val id = timerID
            timer =
                scope.launch {
                    try {
                        scheduler.sleep(1.0)
                        currentCoroutineContext().ensureActive()
                        timerFired(id)
                    } catch (failure: CancellationException) {
                        throw failure
                    } catch (_: Exception) {
                    }
                }
        }

    private suspend fun timerFired(id: Long) {
        if (timerID != id || stopped) return
        timer = null
        try {
            refreshNow()
        } catch (failure: CancellationException) {
            throw failure
        } catch (_: Exception) {
        }
    }

    private fun cancelTimer(): Job? {
        val pending = timer
        val canceledID = timerID
        timerID += 1
        pending?.cancel()
        if (pending != null) {
            canceledTimers[canceledID] = pending
            scope.launch {
                pending.join()
                canceledTimers.remove(canceledID)
            }
        }
        timer = null
        return pending
    }

    suspend fun refreshNow(): SyncResult =
        withContext(confined) {
            currentCoroutineContext().ensureActive()
            epoch.check()
            if (stopped) throw CancellationException()
            database.validate(epoch)
            currentCoroutineContext().ensureActive()
            epoch.check()
            if (stopped) throw CancellationException()
            cancelTimer()
            val running = flight
            val runningWaiters = flightWaiters
            val task: Deferred<SyncResult>
            val waiters: RefreshWaiters
            if (running != null && runningWaiters != null) {
                rerun = true
                task = running
                waiters = runningWaiters
            } else {
                rerun = false
                val runEpoch = epoch.child()
                flightEpoch = runEpoch
                // Enter cleanup even if the last waiter cancels before this flight starts.
                task = scope.async(start = CoroutineStart.ATOMIC) { runFlights(runEpoch) }
                flight = task
                waiters = RefreshWaiters(task, runEpoch)
                flightWaiters = waiters
            }
            val id = UUID.randomUUID()
            waiters.add(id)
            val result =
                try {
                    awaitOwned(task) { waiters.cancel(id) }
                } finally {
                    waiters.remove(id)
                }
            currentCoroutineContext().ensureActive()
            epoch.check()
            result
        }

    private suspend fun runFlights(runEpoch: SyncEpoch): SyncResult {
        try {
            val before = database.snapshot()
            runEpoch.check()
            var after: SyncSnapshot
            var retryAfter: Double? = null
            var serverRetryAfter: Double? = null
            val refusals = mutableListOf<SyncRefusal>()
            var result: SyncResult
            emit(SyncEvent.Started(epoch.accountID, epoch.generation))
            do {
                rerun = false
                runEpoch.check()
                result = runOnce(runEpoch)
                runEpoch.check()
                after = database.snapshot()
                runEpoch.check()
                result.retryAfter?.let { retryAfter = maxOf(retryAfter ?: 0.0, it) }
                result.serverRetryAfter?.let { serverRetryAfter = maxOf(serverRetryAfter ?: 0.0, it) }
                refusals += result.refusals
                if (result.state == SyncState.Unauthorized || result.state == SyncState.VerificationRequired) break
            } while (rerun)
            val merged =
                SyncResult(
                    result.state,
                    result.pendingCount,
                    retryAfter,
                    serverRetryAfter,
                    refusals.toList(),
                    before.changes(after),
                )
            runEpoch.check()
            emit(SyncEvent.Finished(epoch.accountID, epoch.generation, merged))
            return merged
        } finally {
            flight = null
            flightEpoch = null
            flightWaiters = null
            rerun = false
        }
    }

    private suspend fun runOnce(epoch: SyncEpoch): SyncResult {
        val source = source ?: throw CancellationException()
        var state = SyncState.Idle
        var changes = SyncChanges()
        var retryAfter: Double? = null
        var serverRetryAfter: Double? = null
        var refusals: List<SyncRefusal> = emptyList()
        try {
            val online = isOnline()
            epoch.check()
            if (online) {
                val account = database.account()
                epoch.check()
                if (account?.hasVerifiedEmail == false) {
                    val identity = source.fetchIdentity()
                    epoch.check()
                    database.applyIdentity(epoch, identity)
                    epoch.check()
                    if (!identity.hasVerifiedEmail) {
                        val pending = database.pending().size
                        epoch.check()
                        return SyncResult(SyncState.VerificationRequired, pending)
                    }
                }
                val flush = OutboxFlusher(database, source, scheduler.now).run(epoch)
                epoch.check()
                refusals = flush.refusals
                retryAfter = flush.retryAfter
                serverRetryAfter = flush.serverRetryAfter
                if (flush.offline) {
                    state = SyncState.Offline
                } else if (flush.deferredWalkthroughAuthorization) {
                    state = SyncState.Failed
                } else {
                    val pull = FullPull(database, source, retention, scheduler.now).run(epoch)
                    epoch.check()
                    changes = pull.changes
                    if (pull.verificationRequired) state = SyncState.VerificationRequired
                    pull.retryAfter?.let { retryAfter = maxOf(retryAfter ?: 0.0, it) }
                }
            } else {
                state = SyncState.Offline
            }
        } catch (failure: CancellationException) {
            throw failure
        } catch (error: Exception) {
            epoch.check()
            when (val failure = error as? DataFailure) {
                DataFailure.Offline -> {
                    state = SyncState.Offline
                }

                is DataFailure.Unauthorized -> {
                    state = SyncState.Unauthorized
                }

                is DataFailure.EmailUnverified -> {
                    state = SyncState.VerificationRequired
                    database.markEmailUnverified(epoch)
                    epoch.check()
                }

                is DataFailure.Throttled -> {
                    state = SyncState.Failed
                    retryAfter = maxOf(retryAfter ?: 0.0, failure.retryAfterSeconds)
                    serverRetryAfter = maxOf(serverRetryAfter ?: 0.0, failure.retryAfterSeconds)
                }

                else -> {
                    state = SyncState.Failed
                    retryAfter = maxOf(retryAfter ?: 0.0, 1.0)
                }
            }
        }
        val pending = database.pending().size
        epoch.check()
        return SyncResult(state, pending, retryAfter, serverRetryAfter, refusals, changes)
    }

    private fun emit(event: SyncEvent) {
        if (stopped || !epoch.isValid) return
        observers.values.forEach { it.trySend(event) }
    }

    suspend fun cancelAndWait() =
        withContext(NonCancellable + confined) {
            stopped = true
            val details = detailTasks.values.toList()
            details.forEach { (epoch, task) ->
                epoch.invalidate()
                task.cancel()
            }
            val pendingTimer = cancelTimer()
            flightEpoch?.invalidate()
            val pending = flight
            pending?.cancel()
            pending?.join()
            pendingTimer?.join()
            val timers = canceledTimers.values.toList()
            canceledTimers.clear()
            timers.forEach { it.join() }
            details.forEach { (_, task) -> task.join() }
            detailTasks.clear()
            source = null
            observers.values.forEach { it.close() }
            observers.clear()
            scope.cancel()
        }
}

private class RefreshWaiters(
    private val task: Job,
    private val epoch: SyncEpoch,
) {
    private val lock = ReentrantLock()
    private val ids = mutableSetOf<UUID>()

    fun add(id: UUID) {
        lock.withLock { ids.add(id) }
    }

    fun remove(id: UUID) {
        lock.withLock { ids.remove(id) }
    }

    fun cancel(id: UUID) {
        lock.withLock {
            if (!ids.remove(id) || ids.isNotEmpty()) return
            epoch.invalidate()
            task.cancel()
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun <T> awaitOwned(
    task: Deferred<T>,
    onCancel: () -> Unit,
): T =
    suspendCancellableCoroutine { continuation ->
        val completion =
            task.invokeOnCompletion { failure ->
                if (failure == null) {
                    continuation.resumeWith(Result.success(task.getCompleted()))
                } else {
                    continuation.resumeWith(Result.failure(failure))
                }
            }
        continuation.invokeOnCancellation {
            completion.dispose()
            onCancel()
        }
    }
