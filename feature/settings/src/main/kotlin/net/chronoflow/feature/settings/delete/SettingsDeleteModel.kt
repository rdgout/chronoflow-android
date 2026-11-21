package net.chronoflow.feature.settings.delete

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.session.StaleSessionException
import net.chronoflow.feature.settings.security.SecretField
import java.time.Instant
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

data class DeleteState(
    val stage: SettingsDeleteModel.Stage = SettingsDeleteModel.Stage.Confirmation,
    val busy: Boolean = false,
    val errorKey: String? = null,
    val retryAt: Instant? = null,
)

class SettingsDeleteModel(
    private val delete: suspend (String) -> Unit,
    private val now: () -> Instant = Instant::now,
) {
    enum class Stage { Confirmation, Password, Deleted }

    private val mutableState = MutableStateFlow(DeleteState())
    val state = mutableState.asStateFlow()
    private var generation = 0L

    fun confirmDeletion() {
        if (state.value.busy) return
        mutableState.update { it.copy(stage = Stage.Password) }
    }

    suspend fun submit(
        password: SecretField,
        now: Instant = this.now(),
    ) {
        val state = state.value
        if (state.busy || state.stage != Stage.Password || state.retryAt?.isAfter(now) == true) return
        val value = password.read()
        if (value.isEmpty()) {
            mutableState.update { it.copy(errorKey = "Enter your password.") }
            return
        }
        password.clear()
        val started = generation
        mutableState.update { it.copy(busy = true, errorKey = null) }
        try {
            delete(value)
            currentCoroutineContext().ensureActive()
            if (generation == started) mutableState.update { it.copy(stage = Stage.Deleted) }
        } catch (_: StaleSessionException) {
            currentCoroutineContext().ensureActive()
        } catch (failure: CancellationException) {
            throw failure
        } catch (failure: DataFailure) {
            if (generation != started) return
            val key =
                when (failure) {
                    DataFailure.Offline -> {
                        "Needs a connection."
                    }

                    is DataFailure.Throttled -> {
                        mutableState.update {
                            it.copy(
                                retryAt =
                                    now.plus(
                                        failure.retryAfterSeconds
                                            .coerceAtLeast(0.0)
                                            .seconds
                                            .toJavaDuration(),
                                    ),
                            )
                        }
                        "Too many attempts."
                    }

                    is DataFailure.Validation -> {
                        when {
                            failure.fields["billing"] != null -> {
                                "Your subscription could not be cancelled. Please try deleting your account again."
                            }

                            failure.fields["password"]?.key == "The provided password was incorrect." -> {
                                "The provided password was incorrect."
                            }

                            else -> {
                                "Check the highlighted field."
                            }
                        }
                    }

                    else -> {
                        "Something went wrong"
                    }
                }
            mutableState.update { it.copy(errorKey = key) }
        } finally {
            if (generation == started) mutableState.update { it.copy(busy = false) }
        }
    }

    fun stop() {
        generation++
        mutableState.update { it.copy(stage = Stage.Confirmation, busy = false, errorKey = null) }
    }
}
