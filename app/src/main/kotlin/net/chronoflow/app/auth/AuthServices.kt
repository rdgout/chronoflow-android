package net.chronoflow.app.auth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.chronoflow.app.AppDispatchers
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.api.SessionResult
import net.chronoflow.core.model.auth.AuthenticationMethod
import net.chronoflow.core.model.passkey.PasskeyCapability
import net.chronoflow.core.model.passkey.PasskeyFailure
import net.chronoflow.core.navigation.RootNavigator
import net.chronoflow.core.navigation.Route
import net.chronoflow.feature.auth.model.ForgotPasswordModel
import net.chronoflow.feature.auth.model.SessionAcceptor
import net.chronoflow.feature.auth.model.SignInModel
import net.chronoflow.feature.auth.model.passkey.DeterministicAssertionProvider
import net.chronoflow.feature.auth.model.passkey.PasskeyAssertionProviding
import net.chronoflow.feature.auth.model.passkey.PasskeySignIn
import net.chronoflow.feature.auth.signin.SignInRouter

class AuthServices(
    private val navigator: RootNavigator,
    appScope: CoroutineScope,
    private val dispatchers: AppDispatchers,
    private val source: DataSource,
    session: SessionAcceptor,
    deviceIdentity: suspend () -> String,
    deviceName: suspend () -> String,
    routeAcceptedSession: suspend () -> Unit,
    passkeyCapability: PasskeyCapability = PasskeyCapability.Shipped,
    assertionProvider: PasskeyAssertionProviding = DeterministicAssertionProvider { throw PasskeyFailure.Unavailable },
    val mockMail: (suspend (String) -> Boolean)? = null,
) {
    private val nameMutex = Mutex()
    private var cachedName: String? = null
    val router: SignInRouter =
        object : SignInRouter {
            override fun back() {
                navigator.navigate(Route.Welcome)
            }

            override fun forgot() {
                signIn.navigation.forgot()
                startForgot()
                navigator.navigate(Route.Forgot)
            }

            override fun signUp() {
                navigator.navigate(Route.SignUp)
            }

            override fun guest() {
                navigator.navigate(Route.GuestWizard)
            }

            override fun backToSignIn() {
                if (navigator.state.value.route == Route.TwoFactor) navigator.navigate(Route.SignIn)
            }

            override fun twoFactor() {
                navigator.navigate(Route.TwoFactor)
            }

            override suspend fun acceptedSession(
                result: SessionResult,
                method: AuthenticationMethod,
            ) {
                routeAcceptedSession()
            }
        }
    private val identity: suspend () -> String = { withContext(dispatchers.io) { deviceIdentity() } }
    private val name: suspend () -> String = {
        withContext(dispatchers.io) {
            nameMutex.withLock { cachedName ?: deviceName().also { cachedName = it } }
        }
    }
    val signIn: SignInModel by lazy {
        SignInModel(
            source,
            session,
            deviceIdentity = identity,
            deviceName = name,
            passkeyCapability = passkeyCapability,
            passkey = PasskeySignIn(source, assertionProvider, identity, name)::signIn,
            accepted = router::acceptedSession,
            onTwoFactor = router::twoFactor,
            onLeaveTwoFactor = router::backToSignIn,
        )
    }

    var forgotModel: ForgotPasswordModel? = null
        private set

    fun startForgot(): ForgotPasswordModel {
        forgotModel?.cancel()
        return ForgotPasswordModel(source, signIn.navigation, ::toSignIn, signIn.navigation.forgotEmail, mockMail)
            .also { forgotModel = it }
    }

    private fun toSignIn() {
        navigator.navigate(Route.SignIn)
    }

    init {
        var previous = navigator.state.value.route
        appScope.launch(dispatchers.main, start = CoroutineStart.UNDISPATCHED) {
            navigator.state.collect { state ->
                if (navigator.state.value.route != Route.Forgot) {
                    forgotModel?.cancel()
                    forgotModel = null
                }
                if (previous.isAuth() && !state.route.isAuth()) signIn.clearSecrets()
                previous = state.route
            }
        }
    }
}

private fun Route?.isAuth(): Boolean =
    when (this) {
        Route.SignIn, Route.TwoFactor, Route.Forgot, Route.SignUp, is Route.Reset -> true
        else -> false
    }
