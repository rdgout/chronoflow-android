package net.chronoflow.core.sync.refresh

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.sync.SyncResult
import net.chronoflow.core.sync.SyncState

data class RefreshFeedbackState(
    val refreshing: Boolean = false,
    val feedback: List<RefreshFeedback> = emptyList(),
    val presentationId: Long = 0,
)

interface RefreshDebugProbe {
    fun accepted()

    fun outcome(value: String)

    fun presented(text: String)

    companion object {
        val None: RefreshDebugProbe =
            object : RefreshDebugProbe {
                override fun accepted() = Unit

                override fun outcome(value: String) = Unit

                override fun presented(text: String) = Unit
            }
    }
}

class RefreshFeedbackModel(
    private val scope: CoroutineScope,
    private val probe: RefreshDebugProbe = RefreshDebugProbe.None,
) {
    private val mutableState = MutableStateFlow(RefreshFeedbackState())
    val state: StateFlow<RefreshFeedbackState> = mutableState.asStateFlow()

    fun refresh(
        userInitiated: Boolean = true,
        action: suspend () -> SyncResult?,
    ): Job? {
        while (true) {
            val previous = mutableState.value
            if (previous.refreshing) return null
            if (mutableState.compareAndSet(
                    previous,
                    RefreshFeedbackState(
                        refreshing = true,
                        presentationId =
                            previous.presentationId + 1,
                    ),
                )
            ) {
                break
            }
        }
        if (userInitiated) probe.accepted()
        // The gesture can disappear when its refreshed content redraws; the owning scope keeps the work alive.
        return scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                val result = action()
                if (result == null) {
                    probe.outcome("nil")
                } else {
                    val feedback = outcome(result, userInitiated)
                    mutableState.update { it.copy(feedback = feedback) }
                    probe.outcome(
                        "result:${result.state.name.replaceFirstChar { it.lowercaseChar() }}:${feedback.size}",
                    )
                }
            } catch (error: CancellationException) {
                probe.outcome("cancelled")
                throw error
            } catch (error: Exception) {
                probe.outcome("error:${error.javaClass.simpleName}")
                val state = if (error is DataFailure.Offline) SyncState.Offline else SyncState.Failed
                mutableState.update { it.copy(feedback = outcome(SyncResult(state, pendingCount = 0), userInitiated)) }
            } finally {
                mutableState.update { it.copy(refreshing = false) }
            }
        }
    }

    fun dismiss(presentationId: Long) {
        mutableState.update { if (it.presentationId == presentationId) it.copy(feedback = emptyList()) else it }
    }
}
