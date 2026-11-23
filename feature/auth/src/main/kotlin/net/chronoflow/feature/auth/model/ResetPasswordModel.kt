package net.chronoflow.feature.auth.model

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.api.ResetPasswordInput
import net.chronoflow.core.model.auth.ResetPasswordLink
import net.chronoflow.core.model.device.DeviceIdentityFailure
import net.chronoflow.core.model.session.SessionFailure
import net.chronoflow.core.model.store.StoreFailure
import java.io.IOException
import java.security.GeneralSecurityException

class ResetPasswordModel(
    val link: ResetPasswordLink,
    private val source: DataSource,
    private val navigation: AuthNavigation,
    private val toSignIn: () -> Unit,
    mail: (suspend (String) -> Boolean)? = null,
) {
    enum class State { Form, Expired, Done }

    val form = AuthFormModel(setOf("password", "password_confirmation"), AuthFormContext.Reset)
    val recovery = ForgotPasswordModel(source, navigation, toSignIn, link.email, mail)
    private val mutableState = MutableStateFlow(if (link.isValid) State.Form else State.Expired)
    val state: StateFlow<State> = mutableState.asStateFlow()
    var isActive = true
        private set

    fun editPassword() {
        if (isActive && !form.busy) form.edit("password")
    }

    fun editConfirmation() {
        if (isActive && !form.busy) form.edit("password_confirmation")
    }

    suspend fun submit(
        password: SecretField,
        confirmation: SecretField,
    ) {
        if (!isActive || state.value != State.Form || !form.begin(AuthFormModel.Action.Reset)) return
        try {
            currentCoroutineContext().ensureActive()
            source.resetPassword(ResetPasswordInput(link.token, link.email, password.read(), confirmation.read()))
            currentCoroutineContext().ensureActive()
            if (!isActive) return
            mutableState.value = State.Done
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: DataFailure) {
            if (isActive && failure is DataFailure.Validation &&
                failure.fields.values.any {
                    it.key == "This password reset link is invalid or has expired."
                }
            ) {
                mutableState.value = State.Expired
            } else {
                receive(failure)
            }
        } catch (_: SessionFailure) {
            receive(DataFailure.Server(null))
        } catch (_: DeviceIdentityFailure) {
            receive(DataFailure.Server(null))
        } catch (_: StoreFailure) {
            receive(DataFailure.Server(null))
        } catch (_: IOException) {
            receive(DataFailure.Server(null))
        } catch (_: GeneralSecurityException) {
            receive(DataFailure.Server(null))
        } catch (_: RuntimeException) {
            receive(DataFailure.Server(null))
        } finally {
            password.clear()
            confirmation.clear()
            form.finish()
        }
    }

    private fun receive(failure: DataFailure) {
        if (isActive) form.receive(failure)
    }

    fun backToSignIn() {
        cancel()
        navigation.reset()
        toSignIn()
    }

    fun cancel() {
        isActive = false
        form.clear()
        recovery.cancel()
    }
}
