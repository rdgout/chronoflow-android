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
import net.chronoflow.core.model.auth.AuthenticationMethod
import net.chronoflow.core.model.device.DeviceIdentityFailure
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
) {
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
        commitAutofill: () -> Unit,
    ) {
        if (!active || !form.begin(AuthFormModel.Action.Register)) return
        sessionFailure = null
        try {
            val id = deviceIdentity()
            currentCoroutineContext().ensureActive()
            if (!active) return
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
                    ),
                )
            currentCoroutineContext().ensureActive()
            if (!active) return
            // A committed session owns its routing even when the presenting screen disappears.
            withContext(NonCancellable) {
                session.accept(result)
                commitAutofill()
                password.clear()
                confirmation.clear()
                accepted(result, AuthenticationMethod.SignUp)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: SessionFailure) {
            if (!active) return
            sessionFailure = failure
            form.receive(DataFailure.Server(null))
        } catch (failure: DataFailure) {
            if (!active) return
            form.receive(failure)
        } catch (_: DeviceIdentityFailure) {
            if (!active) return
            form.receive(DataFailure.Server(null))
        } catch (_: StoreFailure) {
            if (!active) return
            form.receive(DataFailure.Server(null))
        } catch (_: IOException) {
            if (!active) return
            form.receive(DataFailure.Server(null))
        } catch (_: GeneralSecurityException) {
            if (!active) return
            form.receive(DataFailure.Server(null))
        } catch (_: RuntimeException) {
            if (!active) return
            form.receive(DataFailure.Server(null))
        } finally {
            password.clear()
            confirmation.clear()
            form.finish()
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
