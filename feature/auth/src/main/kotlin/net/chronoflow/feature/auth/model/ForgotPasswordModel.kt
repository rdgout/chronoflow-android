package net.chronoflow.feature.auth.model

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.device.DeviceIdentityFailure
import net.chronoflow.core.model.session.SessionFailure
import net.chronoflow.core.model.store.StoreFailure
import java.io.IOException
import java.security.GeneralSecurityException

class ForgotPasswordModel(
    private val source: DataSource,
    private val navigation: AuthNavigation,
    private val toSignIn: () -> Unit,
    email: String = "",
    private val mail: (suspend (String) -> Boolean)? = null,
) {
    val form = AuthFormModel(setOf("email"), AuthFormContext.Forgot)
    private val mutableEmail = MutableStateFlow(email)
    val email: StateFlow<String> = mutableEmail.asStateFlow()
    private val mutableSentEmail = MutableStateFlow<String?>(null)
    val sentEmail: StateFlow<String?> = mutableSentEmail.asStateFlow()
    var isActive = true
        private set

    fun editEmail(value: String) {
        if (!isActive || form.busy || value == email.value) return
        mutableEmail.value = value
        form.edit("email")
    }

    suspend fun submit() {
        if (!isActive || sentEmail.value != null || !form.begin(AuthFormModel.Action.SendResetLink)) return
        val submitted = email.value
        try {
            currentCoroutineContext().ensureActive()
            source.requestPasswordReset(submitted)
            currentCoroutineContext().ensureActive()
            if (!isActive) return
            mutableSentEmail.value = submitted
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: DataFailure) {
            receive(failure)
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
            form.finish()
        }
    }

    private fun receive(failure: DataFailure) {
        if (isActive) form.receive(failure)
    }

    suspend fun openEmail(open: () -> Boolean) {
        if (!isActive) return
        val sent = sentEmail.value ?: return
        if (mail != null) mail.invoke(sent) else open()
    }

    fun backToSignIn() {
        cancel()
        navigation.reset()
        toSignIn()
    }

    fun cancel() {
        isActive = false
        form.clear()
    }
}
