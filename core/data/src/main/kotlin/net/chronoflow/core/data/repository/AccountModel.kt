package net.chronoflow.core.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.store.AccountRecord

class AccountModel(
    private val scope: CoroutineScope,
) {
    @ConsistentCopyVisibility
    data class AttachmentToken internal constructor(
        val generation: Long,
    )

    sealed interface LoadState {
        data object Unknown : LoadState

        data class Loaded(
            val account: AccountRecord?,
        ) : LoadState

        data object Failed : LoadState
    }

    private val mutableState = MutableStateFlow<LoadState>(LoadState.Unknown)
    val state: StateFlow<LoadState> = mutableState.asStateFlow()
    var epoch: Long = 0
        private set
    private var generation: Long = 0
    val attachmentToken: AttachmentToken get() = AttachmentToken(generation)
    private var subscription: Job? = null
    private val context = scope.coroutineContext.minusKey(Job)

    suspend fun attach(
        repository: AccountRepository,
        epoch: Long,
        ifCurrent: AttachmentToken? = null,
    ) {
        if (!currentCoroutineContext().isActive) return
        withContext(context) {
            if (ifCurrent != null && ifCurrent != attachmentToken) return@withContext
            val previous = cancelSubscription()
            this@AccountModel.epoch = epoch
            val attachedGeneration = generation
            previous?.join()
            if (!currentCoroutineContext().isActive || attachedGeneration != generation) return@withContext
            subscription =
                scope.launch {
                    try {
                        repository.values().collect { value ->
                            if (currentCoroutineContext().isActive && generation == attachedGeneration) {
                                mutableState.value = LoadState.Loaded(value)
                            }
                        }
                        if (currentCoroutineContext().isActive && generation == attachedGeneration) {
                            mutableState.value = LoadState.Unknown
                        }
                    } catch (failure: CancellationException) {
                        throw failure
                    } catch (_: Exception) {
                        failed(attachedGeneration)
                    }
                }
        }
    }

    suspend fun detach() {
        withContext(NonCancellable + context) { cancelSubscription()?.join() }
    }

    private suspend fun failed(attachedGeneration: Long) {
        if (currentCoroutineContext().isActive &&
            generation == attachedGeneration
        ) {
            mutableState.value = LoadState.Failed
        }
    }

    private fun cancelSubscription(): Job? {
        generation += 1
        val previous = subscription
        subscription = null
        previous?.cancel()
        mutableState.value = LoadState.Unknown
        return previous
    }
}
