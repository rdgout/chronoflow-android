package net.chronoflow.app.guest

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.chronoflow.app.auth.AuthServices
import net.chronoflow.core.model.api.AuthLanguage
import net.chronoflow.core.navigation.RootNavigator
import net.chronoflow.core.navigation.Route
import net.chronoflow.feature.auth.guest.GuestSignupFields
import net.chronoflow.feature.auth.model.GuestSignupState
import net.chronoflow.feature.auth.model.SecretField
import net.chronoflow.feature.auth.model.SignUpModel
import net.chronoflow.feature.wizard.guest.GuestSignUpHost
import net.chronoflow.feature.wizard.guest.GuestSignUpLauncher

interface GuestSignupSeeding {
    val state: GuestSignupState
}

interface GuestSignupPrefilling {
    val name: String
    val email: String

    fun fillSecrets(fields: GuestSignupFields)
}

data class GuestSignupPresentation(
    val id: Long,
    val model: SignUpModel,
    val host: GuestSignUpHost,
    val prefill: GuestSignupPrefilling?,
)

class GuestSignupCoordinator(
    private val auth: AuthServices,
    private val navigator: RootNavigator,
    private val revision: Long,
    val seed: GuestSignupSeeding? = null,
    val prefill: GuestSignupPrefilling? = null,
) : GuestSignUpLauncher {
    private val mutablePresentation = MutableStateFlow<GuestSignupPresentation?>(null)
    val presentation = mutablePresentation.asStateFlow()
    private var scope: CoroutineScope? = null
    private var request: Job? = null
    private var nextId = 0L
    private var active = true

    fun bind(scope: CoroutineScope) {
        check(this.scope == null)
        this.scope = scope
    }

    override fun request(host: GuestSignUpHost) {
        if (!active || presentation.value != null) return
        val model = auth.signIn.makeSignUpModel(initialGuestState = seed?.state ?: GuestSignupState.Ready, onBack = {})
        prefill?.let {
            model.editName(it.name)
            model.editEmail(it.email)
        }
        mutablePresentation.value = GuestSignupPresentation(++nextId, model, host, prefill)
    }

    fun submit(
        password: SecretField,
        confirmation: SecretField,
        language: AuthLanguage,
        commitAutofill: () -> Unit,
    ) {
        val shown = presentation.value ?: return
        if (request != null || shown.model.form.busy || shown.model.guestState.value == GuestSignupState.Review) return
        launch {
            shown.model.submit(
                password,
                confirmation,
                language,
                requiresGuestClaim = true,
                commitAutofill = commitAutofill,
            )
        }
    }

    fun dismiss() {
        val shown = presentation.value ?: return
        if (request != null || shown.model.form.busy) return
        val needsReview = shown.model.guestState.value == GuestSignupState.Review
        shown.model.cancel()
        mutablePresentation.value = null
        shown.host.signUpDismissed(needsReview, onGuestRoute())
    }

    fun review() {
        val shown = presentation.value ?: return
        if (request != null || shown.model.form.busy) return
        shown.host.reviewExpired()
        dismiss()
    }

    fun signInInstead() {
        val shown = presentation.value ?: return
        if (request != null || shown.model.form.busy) return
        launch {
            shown.host.prepareSignIn()
            val email = shown.model.email.value
            val needsReview = shown.model.guestState.value == GuestSignupState.Review
            shown.model.cancel()
            auth.signIn.clearSecrets()
            auth.signIn.editEmail(email)
            mutablePresentation.value = null
            navigator.navigate(Route.SignIn)
            shown.host.signUpDismissed(needsReview, onGuestRoute())
        }
    }

    fun clear() {
        active = false
        request?.cancel()
        request = null
        presentation.value?.model?.cancel()
        mutablePresentation.value = null
        scope = null
    }

    private fun launch(block: suspend () -> Unit) {
        val job =
            checkNotNull(scope).launch(start = CoroutineStart.LAZY) {
                try {
                    block()
                } finally {
                    if (request === currentCoroutineContext()[Job]) request = null
                }
            }
        request = job
        job.start()
    }

    private fun onGuestRoute(): Boolean =
        navigator.state.value.let {
            it.route == Route.GuestWizard &&
                it.revision == revision
        }
}
