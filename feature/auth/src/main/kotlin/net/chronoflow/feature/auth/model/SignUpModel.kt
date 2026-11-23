package net.chronoflow.feature.auth.model

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.api.AuthLanguage
import net.chronoflow.core.model.api.RegisterInput
import net.chronoflow.core.model.api.SessionResult
import net.chronoflow.core.model.api.TripWizardErrors
import net.chronoflow.core.model.auth.AuthenticationMethod
import net.chronoflow.core.model.device.DeviceIdentityFailure
import net.chronoflow.core.model.guest.GuestClaim
import net.chronoflow.core.model.session.SessionFailure
import net.chronoflow.core.model.store.StoreFailure
import java.io.IOException
import java.security.GeneralSecurityException

class SignUpModel(
    private val source: DataSource,
    private val session: SessionAcceptor,
    private val deviceIdentity: suspend () -> String,
    private val deviceName: suspend () -> String,
    private val accepted: suspend (SessionResult, AuthenticationMethod) -> Unit,
    private val onBack: () -> Unit,
    private val claim: suspend () -> GuestClaim? = { null },
    private val guestReviewExpired: suspend (GuestClaim) -> Unit = {},
    initialGuestState: GuestSignupState = GuestSignupState.Ready,
) {
    enum class Outcome { Accepted, GuestReviewExpired }

    private val mutableGuestState = MutableStateFlow(initialGuestState)
    val guestState = mutableGuestState.asStateFlow()
    private val mutableName = MutableStateFlow("")
    private val mutableEmail = MutableStateFlow("")
    private val mutableShowsPassword = MutableStateFlow(false)
    val name = mutableName.asStateFlow()
    val email = mutableEmail.asStateFlow()
    val showsPassword = mutableShowsPassword.asStateFlow()
    val form = AuthFormModel(setOf("name", "email", "password", "password_confirmation"), AuthFormContext.SignUp)
    var sessionFailure: SessionFailure? = null
        private set
    private var active = true

    fun editName(value: String) {
        if (!active || form.busy || value == name.value) return
        mutableName.value = value
        form.edit("name")
    }

    fun editEmail(value: String) {
        if (!active || form.busy || value == email.value) return
        mutableEmail.value = value
        form.edit("email")
    }

    fun editPassword() {
        if (!active || form.busy) return
        form.edit("password")
    }

    fun editConfirmation() {
        if (!active || form.busy) return
        form.edit("password_confirmation")
    }

    fun togglePassword() {
        if (!active || form.busy) return
        mutableShowsPassword.value = !showsPassword.value
    }

    suspend fun submit(
        password: SecretField,
        confirmation: SecretField,
        language: AuthLanguage,
        requiresGuestClaim: Boolean = false,
        commitAutofill: () -> Unit,
    ): Outcome? {
        if (!active || !form.begin(AuthFormModel.Action.Register)) return null
        sessionFailure = null
        mutableGuestState.value = GuestSignupState.Ready
        var attemptedClaim: GuestClaim? = null
        var accountCreated = false
        try {
            val id = deviceIdentity()
            val claim = claim()
            if (requiresGuestClaim && claim == null) {
                mutableGuestState.value = GuestSignupState.Review
                return Outcome.GuestReviewExpired
            }
            attemptedClaim = claim
            currentCoroutineContext().ensureActive()
            if (!active) return null
            val result =
                source.signUp(
                    RegisterInput(
                        name.value,
                        email.value,
                        password.read(),
                        confirmation.read(),
                        id,
                        deviceName(),
                        language,
                        draftToken = claim?.draftToken,
                        inputRevision = claim?.inputRevision,
                        reviewRevision = claim?.reviewRevision,
                        guestSecret = claim?.guestSecret,
                    ),
                )
            currentCoroutineContext().ensureActive()
            if (!active) return null
            accountCreated = true
            // A committed session owns its routing even when the presenting screen disappears.
            withContext(NonCancellable) {
                session.accept(result)
                commitAutofill()
                password.clear()
                confirmation.clear()
                accepted(result, AuthenticationMethod.SignUp)
            }
            return Outcome.Accepted
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: SessionFailure) {
            if (!active) return null
            sessionFailure = failure
            receiveFailure(DataFailure.Server(null), accountCreated)
        } catch (failure: DataFailure) {
            if (!active) return null
            if (attemptedClaim != null && TripWizardErrors.map(failure) == DataFailure.ReviewExpired) {
                guestReviewExpired(attemptedClaim)
                mutableGuestState.value = GuestSignupState.Review
                return Outcome.GuestReviewExpired
            }
            receiveFailure(failure, accountCreated)
        } catch (_: DeviceIdentityFailure) {
            if (!active) return null
            receiveFailure(DataFailure.Server(null), accountCreated)
        } catch (_: StoreFailure) {
            if (!active) return null
            receiveFailure(DataFailure.Server(null), accountCreated)
        } catch (_: IOException) {
            if (!active) return null
            receiveFailure(DataFailure.Server(null), accountCreated)
        } catch (_: GeneralSecurityException) {
            if (!active) return null
            receiveFailure(DataFailure.Server(null), accountCreated)
        } catch (_: RuntimeException) {
            if (!active) return null
            receiveFailure(DataFailure.Server(null), accountCreated)
        } finally {
            password.clear()
            confirmation.clear()
            form.finish()
        }
        return null
    }

    private fun receiveFailure(
        failure: DataFailure,
        accountCreated: Boolean,
    ) {
        form.receive(failure)
        mutableGuestState.value =
            when {
                accountCreated -> GuestSignupState.Retry
                form.state.value.banner == null -> GuestSignupState.Ready
                else -> GuestSignupState.Failed
            }
    }

    fun backToSignIn() {
        cancel()
        onBack()
    }

    fun cancel() {
        active = false
        mutableShowsPassword.value = false
        form.clear()
    }

    fun takeAnnouncement(resolve: (AuthText) -> String): String? = form.takeAnnouncement(resolve)
}
