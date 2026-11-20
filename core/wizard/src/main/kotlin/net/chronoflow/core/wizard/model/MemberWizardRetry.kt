package net.chronoflow.core.wizard.model

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.util.UUID

internal fun MemberWizardModel.updateRetrySeconds() {
    if (closed) {
        retrySeconds = 0
        return
    }
    retrySeconds =
        when {
            state.step == MemberWizardStep.checking && checking.failure != null -> {
                val value = checking.retrySeconds(checkingClock.nowMillis())
                if (value == 0) checking = checking.copy(retryAt = null)
                value
            }

            state.step == MemberWizardStep.ready -> {
                val value = state.saveFailure?.seconds(now()) ?: 0
                if (value == 0) state = state.copy(saveFailure = state.saveFailure?.copy(retryAt = null))
                value
            }

            else -> {
                0
            }
        }
}

private val MemberWizardModel.retryDelay: Long get() =
    when (state.step) {
        MemberWizardStep.checking -> {
            minOf(
                1000,
                (
                    (checking.retryAt ?: checkingClock.nowMillis()) -
                        checkingClock.nowMillis()
                ).coerceAtLeast(0),
            )
        }

        MemberWizardStep.ready -> {
            minOf(
                1000,
                state.saveFailure?.retryAt?.let { Duration.between(now(), it).toMillis().coerceAtLeast(0) } ?: 0,
            )
        }

        else -> {
            0
        }
    }

internal fun MemberWizardModel.restartRetryCountdown() {
    retryJob?.cancel()
    retryJob = null
    val id = UUID.randomUUID().toString()
    retryID = id
    updateRetrySeconds()
    if (retrySeconds <= 0) return
    retryJob =
        scope.launch {
            while (true) {
                val delay = retryDelay
                if (delay > 0) checkingClock.sleepUntilMillis(checkingClock.nowMillis() + delay)
                currentCoroutineContext().ensureActive()
                if (retryID != id) return@launch
                updateRetrySeconds()
                if (retrySeconds == 0) {
                    retryJob = null
                    return@launch
                }
            }
        }
}
