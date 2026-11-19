package net.chronoflow.feature.auth.twofactor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.chronoflow.feature.auth.model.AuthFormState
import net.chronoflow.feature.auth.model.AuthText
import net.chronoflow.feature.auth.model.SignInModel
import net.chronoflow.feature.auth.model.TwoFactorModel
import net.chronoflow.feature.auth.signin.SignInRouter

data class TwoFactorUiState(
    val mode: TwoFactorModel.Mode,
    val code: String,
    val form: AuthFormState,
    val field: String,
) {
    override fun toString(): String = "TwoFactorUiState(mode=$mode, code=***, form=$form, field=$field)"
}

@OptIn(ExperimentalCoroutinesApi::class)
class TwoFactorViewModel(
    private val signIn: SignInModel,
    private val router: SignInRouter,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) : ViewModel(scope) {
    val currentModel: TwoFactorModel? get() = signIn.twoFactor.value
    val uiState: StateFlow<TwoFactorUiState?> =
        signIn.twoFactor
            .flatMapLatest { model ->
                model?.let {
                    combine(
                        it.state,
                        it.form.state,
                    ) {
                        s,
                        f,
                        ->
                        TwoFactorUiState(
                            s.mode,
                            s.code,
                            f,
                            it.field,
                        )
                    }
                }
                    ?: flowOf(null)
            }.stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                currentModel?.let {
                    TwoFactorUiState(it.mode, it.code, it.form.state.value, it.field)
                },
            )
    private var request: Job? = null

    fun onCodeEdited(text: String) {
        currentModel?.editCode(text)
    }

    fun toggleMode() {
        currentModel?.toggleMode()
    }

    fun validateChallenge() {
        val model = currentModel
        if (model == null) router.backToSignIn() else model.validateChallenge()
    }

    fun takeAnnouncement(resolve: (AuthText) -> String): String? = currentModel?.form?.takeAnnouncement(resolve)

    fun submit() {
        val model = currentModel ?: return
        if (request != null || model.form.busy) return
        val job =
            viewModelScope.launch(start = CoroutineStart.LAZY) {
                try {
                    model.submit()
                } finally {
                    if (request === currentCoroutineContext()[Job]) request = null
                }
            }
        request = job
        job.start()
    }

    fun back() {
        request?.cancel()
        request = null
        currentModel?.cancel()
        router.backToSignIn()
    }
}
