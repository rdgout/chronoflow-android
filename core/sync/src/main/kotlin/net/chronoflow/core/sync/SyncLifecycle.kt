package net.chronoflow.core.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.session.SyncEpoch
import java.time.Duration
import java.time.Instant

data class SyncRetryState(
    val failures: Int = 0,
    val serverDeadline: Instant? = null,
    val lastResult: SyncResult? = null,
)

class SyncLifecycle(
    private val database: SyncDatabase,
    source: DataSource,
    private val epoch: SyncEpoch,
    private val scheduler: SyncScheduler,
    retryState: SyncRetryState = SyncRetryState(),
    private val path: SyncPathMonitoring? = null,
    retention: PlanRetention = PlanRetention(),
    private val unauthorized: suspend (Long) -> Unit = { },
    private val verification: suspend (Long) -> Unit = { },
    dispatcher: CoroutineDispatcher,
) {
    private val confined = dispatcher.limitedParallelism(1)
    private val scope = CoroutineScope(SupervisorJob() + confined)
    val engine = SyncEngine(database, source, epoch, retention, scheduler, dispatcher = dispatcher)

    @Volatile var retryState: SyncRetryState = retryState
        private set
    private var failures: Int
        get() = retryState.failures
        set(value) {
            retryState = retryState.copy(failures = value)
        }
    private var serverDeadline: Instant?
        get() = retryState.serverDeadline
        set(value) {
            retryState = retryState.copy(serverDeadline = value)
        }
    private var lastResult: SyncResult?
        get() = retryState.lastResult
        set(value) {
            retryState = retryState.copy(lastResult = value)
        }
    private val listeners = mutableListOf<Job>()
    private var retry: Job? = null
    private val canceledTimers = mutableMapOf<Long, Job>()
    private var retryID = 0L
    private var started = false
    private var stopped = false
    private var unauthorizedSent = false

    suspend fun start() =
        withContext(confined) {
            if (started || stopped) return@withContext
            started = true
            val attempts = database.pending().maxOfOrNull { it.attempts } ?: 0
            failures = maxOf(failures, attempts)
            val events = engine.events()
            val rows = database.outboxValues()
            listeners +=
                scope.launch {
                    events.collect { event ->
                        currentCoroutineContext().ensureActive()
                        if (event is SyncEvent.Finished) finished(event.result)
                    }
                }
            listeners +=
                scope.launch {
                    var previous = emptySet<String>()
                    try {
                        rows.collect { value ->
                            currentCoroutineContext().ensureActive()
                            val ids = value.map { it.id }.toSet()
                            if ((ids - previous).isNotEmpty()) requestSync()
                            previous = ids
                        }
                    } catch (failure: CancellationException) {
                        throw failure
                    } catch (_: Exception) {
                    }
                }
            if (path != null) {
                val updates = path.updates()
                listeners +=
                    scope.launch {
                        var wasOffline = false
                        try {
                            updates.collect { online ->
                                currentCoroutineContext().ensureActive()
                                if (!online) {
                                    wasOffline = true
                                } else if (wasOffline) {
                                    wasOffline = false
                                    requestSync()
                                }
                            }
                        } catch (failure: CancellationException) {
                            throw failure
                        } catch (_: Exception) {
                        }
                    }
            }
            requestSync()
        }

    suspend fun requestSync() =
        withContext(confined) {
            if (stopped || unauthorizedSent) return@withContext
            val deadline = serverDeadline
            if (deadline != null && deadline > scheduler.now() && retry != null) return@withContext
            cancelRetry()
            if (deadline != null && deadline > scheduler.now()) {
                val remaining = Duration.between(scheduler.now(), deadline)
                scheduleRetry(maxOf(1.0, remaining.seconds.toDouble() + remaining.nano / 1_000_000_000.0))
            } else {
                engine.requestSync()
            }
        }

    suspend fun refreshNow(): SyncResult =
        withContext(confined) {
            epoch.check()
            if (stopped || unauthorizedSent) throw CancellationException()
            val deadline = serverDeadline
            val last = lastResult
            if (deadline != null && deadline > scheduler.now() && last != null) return@withContext last
            cancelRetry()
            engine.refreshNow()
        }

    private fun finished(result: SyncResult) {
        if (stopped || !epoch.isValid) return
        lastResult = result
        when (result.state) {
            SyncState.Unauthorized -> {
                if (unauthorizedSent) return
                unauthorizedSent = true
                cancelRetry()
                // Wipe drains the event listener, so dispatch outside that listener.
                scope.launch { if (epoch.isValid) unauthorized(epoch.generation) }
            }

            SyncState.VerificationRequired -> {
                failures = 0
                cancelRetry()
                scope.launch { if (epoch.isValid) verification(epoch.generation) }
            }

            SyncState.Offline -> {
                cancelRetry()
            }

            SyncState.Idle, SyncState.Failed -> {
                if (result.state == SyncState.Idle && result.retryAfter == null) {
                    failures = 0
                    serverDeadline = null
                    cancelRetry()
                    return
                }
                val exponential = OutboxFailurePolicy.retryDelay(failures)
                failures += 1
                val delay = maxOf(exponential, result.retryAfter ?: 0.0)
                serverDeadline = result.serverRetryAfter?.let { scheduler.now().plusMillis((it * 1000).toLong()) }
                scheduleRetry(delay)
            }
        }
    }

    private fun scheduleRetry(delay: Double) {
        cancelRetry()
        val id = retryID
        retry =
            scope.launch {
                try {
                    scheduler.sleep(delay)
                    currentCoroutineContext().ensureActive()
                    retryFired(id)
                } catch (failure: CancellationException) {
                    throw failure
                } catch (_: Exception) {
                }
            }
    }

    private suspend fun retryFired(id: Long) {
        if (stopped || retryID != id) return
        retry = null
        serverDeadline = null
        try {
            engine.refreshNow()
        } catch (failure: CancellationException) {
            throw failure
        } catch (_: Exception) {
        }
    }

    private fun cancelRetry(): Job? {
        val old = retry
        val canceledID = retryID
        retryID += 1
        old?.cancel()
        if (old != null) {
            canceledTimers[canceledID] = old
            scope.launch {
                old.join()
                canceledTimers.remove(canceledID)
            }
        }
        retry = null
        return old
    }

    suspend fun stop() =
        withContext(NonCancellable + confined) {
            stopped = true
            epoch.invalidate()
            val timer = cancelRetry()
            val old = listeners.toList()
            listeners.clear()
            old.forEach { it.cancel() }
            path?.stop()
            engine.cancelAndWait()
            old.forEach { it.join() }
            timer?.join()
            val timers = canceledTimers.values.toList()
            canceledTimers.clear()
            timers.forEach { it.join() }
        }
}
