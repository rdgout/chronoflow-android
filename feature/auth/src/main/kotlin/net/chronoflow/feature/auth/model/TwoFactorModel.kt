package net.chronoflow.feature.auth.model

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.api.SessionResult
import net.chronoflow.core.model.api.TwoFactorInput
import net.chronoflow.core.model.session.SessionFailure
import java.io.IOException

data class TwoFactorState(
    val mode: TwoFactorModel.Mode = TwoFactorModel.Mode.Code,
    val code: String = "",
    val sessionFailure: SessionFailure? = null,
) {
    override fun toString(): String =
        "TwoFactorState(mode=$mode, code=${if (code.isEmpty()) "" else "***"}, sessionFailure=$sessionFailure)"
}

class TwoFactorModel(
    private val source: DataSource,
    private val navigation: AuthNavigation,
    private val accept: suspend (SessionResult) -> Unit,
    private val leave: () -> Unit,
) {
    enum class Mode { Code, Recovery }

    private val mutableState = MutableStateFlow(TwoFactorState())
    val state: StateFlow<TwoFactorState> = mutableState.asStateFlow()
    val mode: Mode get() = state.value.mode
    val code: String get() = state.value.code
    val sessionFailure: SessionFailure? get() = state.value.sessionFailure
    val form = AuthFormModel(setOf("code"), AuthFormContext.TwoFactor)
    val field: String get() = if (mode == Mode.Code) "code" else "recovery_code"
    private var active = true

    fun editCode(value: String) {
        if (!active || form.busy) return
        val next = if (mode == Mode.Code) filterCode(value) else value
        if (next == code) return
        mutableState.value = state.value.copy(code = next)
        form.edit(field)
    }

    fun toggleMode() {
        if (!active || form.busy) return
        mutableState.value = TwoFactorState(if (mode == Mode.Code) Mode.Recovery else Mode.Code)
        form.showFields(setOf(field))
    }

    fun validateChallenge() {
        if (navigation.challengeID.isNullOrBlank()) cancel()
    }

    suspend fun submit() {
        validateChallenge()
        val challenge = navigation.challengeID
        if (!active || challenge == null || !form.begin(AuthFormModel.Action.Verify)) return
        mutableState.value = state.value.copy(sessionFailure = null)
        val proof = if (mode == Mode.Code) TwoFactorInput.Proof.Code(code) else TwoFactorInput.Proof.RecoveryCode(code)
        try {
            currentCoroutineContext().ensureActive()
            val result = source.completeTwoFactor(TwoFactorInput(challenge, proof))
            currentCoroutineContext().ensureActive()
            if (!active || navigation.challengeID != challenge) return
            mutableState.value = state.value.copy(code = "")
            // A committed acceptance must finish discarding its challenge even when the caller was cancelled.
            withContext(NonCancellable) {
                accept(result)
                navigation.discardChallenge()
                active = false
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: SessionFailure) {
            if (active) {
                mutableState.value = state.value.copy(sessionFailure = failure)
                form.receive(DataFailure.Server(null))
            }
        } catch (failure: DataFailure) {
            if (active) form.receive(failure)
        } catch (_: IOException) {
            if (active) form.receive(DataFailure.Server(null))
        } catch (_: RuntimeException) {
            if (active) form.receive(DataFailure.Server(null))
        } finally {
            mutableState.value = state.value.copy(code = "")
            form.finish()
        }
    }

    fun cancel() {
        if (!active) return
        active = false
        mutableState.value = state.value.copy(code = "", sessionFailure = null)
        form.clear()
        navigation.cancelTwoFactor()
        leave()
    }

    companion object {
        fun filterCode(value: CharSequence): String = value.filter { it in '0'..'9' }.take(6).toString()
    }
}
