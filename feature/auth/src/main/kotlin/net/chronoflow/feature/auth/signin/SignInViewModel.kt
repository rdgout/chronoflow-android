package net.chronoflow.feature.auth.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.chronoflow.feature.auth.model.AuthFormState
import net.chronoflow.feature.auth.model.AuthText
import net.chronoflow.feature.auth.model.SecretField
import net.chronoflow.feature.auth.model.SignInModel

data class SignInUiState(
    val form: AuthFormState = AuthFormState(),
    val passkeyVisible: Boolean = false,
)

sealed interface SignInEvent {
    data class EditEmail(
        val value: String,
    ) : SignInEvent

    data object EditPassword : SignInEvent

    data object Password : SignInEvent

    data object Passkey : SignInEvent

    data object Back : SignInEvent

    data object Forgot : SignInEvent

    data object SignUp : SignInEvent

    data object Guest : SignInEvent
}

class SignInViewModel(
    private val model: SignInModel,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) : ViewModel(scope) {
    val email: String get() = model.email.value
    val uiState: StateFlow<SignInUiState> =
        combine(model.email, model.form.state) { _, form ->
            SignInUiState(form, model.passkeyCapability.enabled)
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            SignInUiState(model.form.state.value, model.passkeyCapability.enabled),
        )
    private var request: Job? = null

    fun submitPassword(
        secret: SecretField,
        commitAutofill: () -> Unit,
    ) {
        submit { model.submitPassword(secret, commitAutofill) }
    }

    fun submitPasskey(secret: SecretField) {
        submit { model.submitPasskey(secret) }
    }

    private fun submit(action: suspend () -> Unit) {
        if (request != null || model.form.busy) return
        val job =
            viewModelScope.launch(start = CoroutineStart.LAZY) {
                try {
                    action()
                } finally {
                    if (request === currentCoroutineContext()[Job]) request = null
                }
            }
        request = job
        job.start()
    }

    fun leave() {
        request?.cancel()
        request = null
    }

    fun editEmail(value: String) {
        model.editEmail(value)
    }

    fun editPassword() {
        model.editPassword()
    }

    fun takeAnnouncement(resolve: (AuthText) -> String): String? = model.form.takeAnnouncement(resolve)
}
