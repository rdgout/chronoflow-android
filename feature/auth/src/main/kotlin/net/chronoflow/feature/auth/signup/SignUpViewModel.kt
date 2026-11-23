package net.chronoflow.feature.auth.signup

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
import net.chronoflow.core.model.api.AuthLanguage
import net.chronoflow.feature.auth.model.AuthFormState
import net.chronoflow.feature.auth.model.AuthText
import net.chronoflow.feature.auth.model.SecretField
import net.chronoflow.feature.auth.model.SignUpModel

data class SignUpUiState(
    val name: String = "",
    val email: String = "",
    val showsPassword: Boolean = false,
    val form: AuthFormState = AuthFormState(),
)

sealed interface SignUpEvent {
    data class EditName(
        val value: String,
    ) : SignUpEvent

    data class EditEmail(
        val value: String,
    ) : SignUpEvent

    data object EditPassword : SignUpEvent

    data object EditConfirmation : SignUpEvent

    data object TogglePassword : SignUpEvent

    data object Submit : SignUpEvent

    data object Back : SignUpEvent
}

class SignUpViewModel(
    private val model: SignUpModel,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) : ViewModel(scope) {
    val uiState: StateFlow<SignUpUiState> =
        combine(
            model.name,
            model.email,
            model.showsPassword,
            model.form.state,
        ) { name, email, showsPassword, form -> SignUpUiState(name, email, showsPassword, form) }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                SignUpUiState(model.name.value, model.email.value, model.showsPassword.value, model.form.state.value),
            )
    private var request: Job? = null

    fun submit(
        password: SecretField,
        confirmation: SecretField,
        language: AuthLanguage,
        commitAutofill: () -> Unit,
    ) {
        if (request != null || model.form.busy) return
        val job =
            viewModelScope.launch(start = CoroutineStart.LAZY) {
                try {
                    model.submit(password, confirmation, language, commitAutofill = commitAutofill)
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

    fun backToSignIn() {
        leave()
        model.backToSignIn()
    }

    fun editName(value: String) = model.editName(value)

    fun editEmail(value: String) = model.editEmail(value)

    fun editPassword() = model.editPassword()

    fun editConfirmation() = model.editConfirmation()

    fun togglePassword() = model.togglePassword()

    fun takeAnnouncement(resolve: (AuthText) -> String): String? = model.takeAnnouncement(resolve)

    override fun onCleared() {
        leave()
        model.cancel()
    }
}
