package net.chronoflow.core.sync.session

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.launch.Gate
import net.chronoflow.core.model.launch.LaunchReadiness
import net.chronoflow.core.model.launch.SignedOutReadiness
import net.chronoflow.core.model.session.SessionFailure
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.model.session.StoredAccountReadiness

fun interface AccountReadinessSource {
    suspend fun values(accountId: String): Flow<StoredAccountReadiness?>
}

class StoredDataReadiness(
    private val controller: SessionController,
    private val values: AccountReadinessSource,
    io: CoroutineDispatcher,
) : LaunchReadiness {
    private val confined = io.limitedParallelism(1)
    private val scope = CoroutineScope(SupervisorJob() + confined)
    private var restoration: Deferred<SessionPresence>? = null
    private val revision = MutableStateFlow(0L)
    private val allowedEpoch = MutableStateFlow<Long?>(null)
    val firstLoadEpoch: Long? get() = allowedEpoch.value

    @Volatile var failure: SessionFailure? = null
        private set

    override suspend fun session(): SessionPresence =
        withContext(confined) {
            if (restoration?.isCancelled == true) restoration = null
            val task = restoration ?: scope.async { restore() }.also { restoration = it }
            try {
                task.await()
            } catch (cancelled: CancellationException) {
                task.cancel()
                throw cancelled
            }
        }

    private suspend fun restore(): SessionPresence {
        while (currentCoroutineContext().isActive) {
            val before = revision.value
            try {
                val presence = controller.restore()
                currentCoroutineContext().ensureActive()
                failure = null
                return presence
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                failure = error as? SessionFailure ?: SessionFailure.DatabaseUnavailable
                try {
                    revision.first { it != before }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                }
            }
        }
        return SessionPresence.SignedOut
    }

    suspend fun retry() = withContext(confined) { revision.value += 1 }

    override suspend fun gate(): Gate = gateUpdates().firstOrNull() ?: Gate.App

    override suspend fun hasStoredData(): Boolean = updates().firstOrNull()?.hasStoredData ?: false

    private fun updates(): Flow<StoredAccountReadiness?> =
        flow {
            val presence = controller.presence
            if (presence !is SessionPresence.SignedIn) {
                emit(null)
                return@flow
            }
            val epoch = controller.epoch
            while (currentCoroutineContext().isActive) {
                val before = revision.value
                try {
                    emitAll(
                        values.values(presence.accountId).map { value ->
                            if (controller.epoch != epoch) throw EpochChanged()
                            val mismatched = value != null && value.accountId != presence.accountId
                            if (mismatched) throw SessionFailure.DatabaseUnavailable
                            failure = null
                            value
                        },
                    )
                    return@flow
                } catch (_: EpochChanged) {
                    return@flow
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    if (controller.epoch != epoch) return@flow
                    failure = SessionFailure.DatabaseUnavailable
                    revision.first { it != before }
                }
            }
        }.flowOn(confined)

    override fun gateUpdates(): Flow<Gate> =
        channelFlow {
            if (controller.presence !is SessionPresence.SignedIn) return@channelFlow
            val epoch = controller.epoch
            var fallback: Gate? = null
            var last: Gate? = null

            fun emitGate(gate: Gate) {
                if (controller.epoch == epoch && gate != last) {
                    last = gate
                    trySend(gate)
                }
            }
            val allowance =
                launch {
                    allowedEpoch.collect { allowed ->
                        if (allowed == epoch) fallback?.let(::emitGate)
                    }
                }
            try {
                updates().collect { value ->
                    val gate = value?.gate
                    if (gate != null) {
                        fallback = null
                        emitGate(gate)
                    } else {
                        val next = value?.firstLoadGate ?: Gate.App
                        fallback = next
                        if (allowedEpoch.value == epoch) emitGate(next)
                    }
                }
            } finally {
                allowance.cancel()
            }
        }.buffer(Channel.CONFLATED).flowOn(confined)

    override fun storedDataUpdates(): Flow<Boolean> = updates().map { it?.hasStoredData ?: false }

    override suspend fun allowFirstLoad() =
        withContext(confined) {
            if (controller.presence is SessionPresence.SignedIn) allowedEpoch.value = controller.epoch
        }

    suspend fun routeAcceptedSession(
        navigate: (Gate) -> Unit,
        verification: suspend (Long) -> Unit,
    ) {
        // A committed session owns routing after the submitting screen is cancelled.
        scope
            .async {
                val current = controller.activeSession() ?: return@async
                val gate = gate()
                if (controller.activeSession() != current) return@async
                if (gate == Gate.Verify) verification(current.epoch) else navigate(gate)
            }.await()
    }

    fun close() {
        scope.cancel()
    }

    private class EpochChanged : CancellationException()
}

object UnavailableReadiness : LaunchReadiness by SignedOutReadiness {
    override suspend fun session(): SessionPresence = awaitCancellation()

    override suspend fun hasStoredData() = false
}
