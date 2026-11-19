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
import net.chronoflow.core.model.api.PasswordSignInInput
import net.chronoflow.core.model.api.SessionResult
import net.chronoflow.core.model.api.SignInResult
import net.chronoflow.core.model.auth.AuthenticationMethod
import net.chronoflow.core.model.device.DeviceIdentityFailure
import net.chronoflow.core.model.passkey.PasskeyCapability
import net.chronoflow.core.model.passkey.PasskeyFailure
import net.chronoflow.core.model.session.SessionFailure
import net.chronoflow.core.model.store.StoreFailure
import java.io.IOException
import java.security.GeneralSecurityException

class SignInModel(
    private val source: DataSource,
    private val session: SessionAcceptor,
    private val deviceIdentity: suspend () -> String,
    private val deviceName: suspend () -> String,
    val passkeyCapability: PasskeyCapability = PasskeyCapability.Shipped,
    private val passkey: suspend () -> SessionResult = { throw PasskeyFailure.Unavailable },
    private val accepted: suspend (SessionResult, AuthenticationMethod) -> Unit,
    private val onTwoFactor: () -> Unit,
    private val onLeaveTwoFactor: () -> Unit = {},
) {
    private val mutableEmail = MutableStateFlow("")
    val email: StateFlow<String> = mutableEmail.asStateFlow()
    val form: AuthFormModel = AuthFormModel()
    val navigation: AuthNavigation = AuthNavigation()
    private val mutableTwoFactor = MutableStateFlow<TwoFactorModel?>(null)
    val twoFactor: StateFlow<TwoFactorModel?> = mutableTwoFactor.asStateFlow()
    var sessionFailure: SessionFailure? = null
        private set

    fun editEmail(value: String) {
        if (form.busy || value == email.value) return
        mutableEmail.value = value
        form.edit("email")
    }

    fun editPassword() {
        if (form.busy) return
        form.edit("password")
    }

    suspend fun submitPassword(
        password: SecretField,
        commitAutofill: () -> Unit,
    ) {
        attempt(AuthFormModel.Action.Password, password) {
            val id = deviceIdentity()
            currentCoroutineContext().ensureActive()
            val result =
                source.signIn(
                    PasswordSignInInput(
                        email.value,
                        password.read(),
                        id,
                        deviceName(),
                        draftToken = null,
                        guestSecret = null,
                    ),
                )
            currentCoroutineContext().ensureActive()
            when (result) {
                is SignInResult.Session -> {
                    accept(
                        result.result,
                        AuthenticationMethod.Password,
                        commitAutofill,
                        password,
                    )
                }

                is SignInResult.TwoFactor -> {
                    if (navigation.twoFactor(result.challengeId)) {
                        mutableTwoFactor.value =
                            TwoFactorModel(
                                source,
                                navigation,
                                accept = { accept(it, AuthenticationMethod.TwoFactor, {}, null) },
                                leave = onLeaveTwoFactor,
                            )
                        onTwoFactor()
                    }
                }
            }
        }
    }

    suspend fun submitPasskey(password: SecretField) {
        if (!passkeyCapability.enabled) return
        attempt(AuthFormModel.Action.Passkey, password) {
            accept(passkey(), AuthenticationMethod.Passkey, {}, password)
        }
    }

    private suspend fun attempt(
        action: AuthFormModel.Action,
        password: SecretField,
        submit: suspend () -> Unit,
    ) {
        if (!form.begin(action)) return
        sessionFailure = null
        try {
            submit()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: PasskeyFailure.Canceled) {
        } catch (failure: SessionFailure) {
            sessionFailure = failure
            form.receive(DataFailure.Server(null))
        } catch (failure: DataFailure) {
            form.receive(failure)
        } catch (_: PasskeyFailure) {
            form.receive(DataFailure.Server(null))
        } catch (_: DeviceIdentityFailure) {
            form.receive(DataFailure.Server(null))
        } catch (_: StoreFailure) {
            form.receive(DataFailure.Server(null))
        } catch (_: IOException) {
            form.receive(DataFailure.Server(null))
        } catch (_: GeneralSecurityException) {
            form.receive(DataFailure.Server(null))
        } catch (_: RuntimeException) {
            form.receive(DataFailure.Server(null))
        } finally {
            password.clear()
            form.finish()
        }
    }

    private suspend fun accept(
        result: SessionResult,
        method: AuthenticationMethod,
        commitAutofill: () -> Unit,
        password: SecretField?,
    ) {
        currentCoroutineContext().ensureActive()
        // Once the session commits, its routing must survive cancellation of the screen's request.
        withContext(NonCancellable) {
            session.accept(result)
            commitAutofill()
            password?.clear()
            accepted(result, method)
        }
    }

    fun makeSignUpModel(onBack: () -> Unit): SignUpModel =
        SignUpModel(source, session, deviceIdentity, deviceName, accepted, onBack)

    fun prepareReset() {
        twoFactor.value?.cancel()
        mutableTwoFactor.value = null
        navigation.reset()
    }

    fun clearSecrets() {
        twoFactor.value?.cancel()
        mutableTwoFactor.value = null
        navigation.reset()
        form.clear()
    }
}
