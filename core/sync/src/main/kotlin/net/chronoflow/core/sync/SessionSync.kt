package net.chronoflow.core.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.api.AccountSecurityChange
import net.chronoflow.core.model.api.AccountSecurityResult
import net.chronoflow.core.model.session.StoredSession
import net.chronoflow.core.model.session.SyncEpoch
import java.util.UUID

class SessionSync(
    private val accounts: ActiveSyncDatabase,
    private val scheduler: SyncScheduler,
    private val makePath: () -> SyncPathMonitoring? = { null },
    private val source: suspend (StoredSession) -> DataSource,
    private val isCurrent: suspend (StoredSession, Long) -> Boolean = { _, _ -> true },
    private val unauthorized: suspend (Long) -> Unit,
    private val verification: suspend (Long) -> Unit,
    private val dispatcher: CoroutineDispatcher,
) {
    val retention = PlanRetention()
    private val confined = dispatcher.limitedParallelism(1)
    private val scope = CoroutineScope(SupervisorJob() + confined)
    private var lifecycle: SyncLifecycle? = null
    private var eventTask: Job? = null
    private val observers = mutableMapOf<UUID, Channel<SyncEvent>>()
    private var latestEvent: SyncEvent? = null
    private var enabled = true
    private var generation = 0L
    private var retryContext: Pair<Long, SyncRetryState>? = null
    private var attachedEpoch: Long? = null

    suspend fun start(
        session: StoredSession,
        epoch: Long,
    ) = withContext(confined) {
        val current = isCurrent(session, epoch)
        if (!enabled || !current) return@withContext
        val attached = lifecycle
        if (attachedEpoch == epoch && attached != null) {
            attached.requestSync()
            return@withContext
        }
        generation += 1
        val generation = generation
        latestEvent = null
        val old = lifecycle
        lifecycle = null
        val oldEvents = eventTask
        eventTask = null
        oldEvents?.cancel()
        withContext(NonCancellable) {
            oldEvents?.join()
            old?.stop()
        }
        if (this@SessionSync.generation != generation) return@withContext
        val database = accounts.activeDatabase() ?: return@withContext
        val id =
            try {
                AccountID.of(session.accountId)
            } catch (_: AccountID.InvalidUUID) {
                return@withContext
            }
        val source = source(session)
        val stillCurrent = isCurrent(session, epoch)
        if (!enabled || this@SessionSync.generation != generation || !stillCurrent) return@withContext
        val context = retryContext
        val lifecycle =
            SyncLifecycle(
                database,
                source,
                SyncEpoch(id, epoch),
                scheduler,
                retryState = if (context?.first == epoch) context.second else SyncRetryState(),
                path = makePath(),
                retention = retention,
                unauthorized = unauthorized,
                verification = verification,
                dispatcher = dispatcher,
            )
        this@SessionSync.lifecycle = lifecycle
        attachedEpoch = epoch
        retryContext = null
        val events = lifecycle.engine.events()
        if (this@SessionSync.generation != generation) return@withContext
        eventTask =
            scope.launch {
                events.collect { event ->
                    currentCoroutineContext().ensureActive()
                    publish(event, generation)
                }
            }
        try {
            lifecycle.start()
        } catch (failure: CancellationException) {
            withContext(NonCancellable) {
                lifecycle.stop()
                if (this@SessionSync.generation == generation) this@SessionSync.lifecycle = null
            }
            throw failure
        } catch (_: Exception) {
            withContext(NonCancellable) {
                lifecycle.stop()
                if (this@SessionSync.generation == generation) this@SessionSync.lifecycle = null
            }
        }
        if (this@SessionSync.generation != generation) lifecycle.stop()
    }

    suspend fun resume(
        session: StoredSession,
        epoch: Long,
    ) = withContext(confined) {
        if (!enabled || !isCurrent(session, epoch)) return@withContext
        val attached = lifecycle
        if (attachedEpoch == epoch && attached != null) attached.requestSync() else start(session, epoch)
    }

    suspend fun setEnabled(enabled: Boolean) =
        withContext(if (enabled) confined else NonCancellable + confined) {
            this@SessionSync.enabled = enabled
            if (!enabled) detach(preservingRetry = true)
        }

    suspend fun requestSync() {
        withContext(confined) { lifecycle?.requestSync() }
    }

    suspend fun refreshNow(): SyncResult? = withContext(confined) { lifecycle?.refreshNow() }

    suspend fun stop() = withContext(NonCancellable + confined) { detach(preservingRetry = false) }

    suspend fun deleteAccount(
        password: String,
        session: StoredSession,
        epoch: Long,
    ) = withContext(confined) {
        if (!enabled || !isCurrent(session, epoch)) throw CancellationException()
        if (attachedEpoch != epoch || lifecycle == null) start(session, epoch)
        val lifecycle = lifecycle
        if (!enabled || attachedEpoch != epoch || lifecycle == null || !isCurrent(session, epoch)) {
            throw CancellationException()
        }
        try {
            lifecycle.engine.deleteAccount(password)
        } catch (failure: DataFailure) {
            if (failure is DataFailure.Unauthorized) unauthorized(epoch)
            if (failure is DataFailure.EmailUnverified) verification(epoch)
            throw failure
        }
    }

    suspend fun accountSecurity(
        change: AccountSecurityChange,
        session: StoredSession,
        epoch: Long,
    ): AccountSecurityResult =
        withContext(confined) {
            if (!enabled || !isCurrent(session, epoch)) throw CancellationException()
            if (attachedEpoch != epoch || lifecycle == null) start(session, epoch)
            val lifecycle = lifecycle
            if (!enabled || attachedEpoch != epoch || lifecycle == null || !isCurrent(session, epoch)) {
                throw CancellationException()
            }
            try {
                val result = lifecycle.engine.accountSecurity(change)
                if (!isCurrent(session, epoch)) throw CancellationException()
                result
            } catch (failure: DataFailure) {
                if (!isCurrent(session, epoch)) throw CancellationException()
                if (failure is DataFailure.Unauthorized) unauthorized(epoch)
                if (failure is DataFailure.EmailUnverified) verification(epoch)
                throw failure
            }
        }

    suspend fun fetchDetail(
        tripId: String,
        session: StoredSession,
        epoch: Long,
    ) = withContext(confined) {
        if (!enabled || !isCurrent(session, epoch)) throw CancellationException()
        if (attachedEpoch != epoch || lifecycle == null) start(session, epoch)
        val lifecycle = lifecycle
        if (!enabled || attachedEpoch != epoch || lifecycle == null || !isCurrent(session, epoch)) {
            throw CancellationException()
        }
        try {
            lifecycle.engine.fetchDetail(tripId)
        } catch (failure: DataFailure) {
            if (!isCurrent(session, epoch)) throw CancellationException()
            if (failure is DataFailure.Unauthorized) unauthorized(epoch)
            if (failure is DataFailure.EmailUnverified) verification(epoch)
            throw failure
        }
    }

    fun events(): Flow<SyncEvent> =
        flow {
            val id = UUID.randomUUID()
            val channel = Channel<SyncEvent>(Channel.CONFLATED)
            try {
                withContext(confined) {
                    observers[id] = channel
                    latestEvent?.let { channel.trySend(it) }
                }
                emitAll(channel.receiveAsFlow())
            } finally {
                withContext(NonCancellable + confined) {
                    observers.remove(id)
                    channel.close()
                }
            }
        }

    private fun publish(
        event: SyncEvent,
        generation: Long,
    ) {
        if (this.generation != generation) return
        latestEvent = event
        observers.values.forEach { it.trySend(event) }
    }

    private suspend fun detach(preservingRetry: Boolean) {
        generation += 1
        val generation = generation
        val epoch = attachedEpoch
        attachedEpoch = null
        val previous = lifecycle
        lifecycle = null
        latestEvent = null
        val previousEvents = eventTask
        eventTask = null
        previousEvents?.cancel()
        withContext(NonCancellable) {
            previousEvents?.join()
            if (!preservingRetry) {
                retryContext = null
                retention.clear()
            }
            previous?.stop()
            if (preservingRetry && this@SessionSync.generation == generation && epoch != null && previous != null) {
                retryContext = epoch to previous.retryState
            }
        }
    }
}
