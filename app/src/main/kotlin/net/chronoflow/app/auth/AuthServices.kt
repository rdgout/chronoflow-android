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
import net.chronoflow.core.navigation.RootNavigator
import net.chronoflow.core.navigation.Route
import net.chronoflow.feature.auth.model.SessionAcceptor
import net.chronoflow.feature.auth.model.SignInModel
import net.chronoflow.feature.auth.signin.SignInRouter

class AuthServices(
    private val navigator: RootNavigator,
    appScope: CoroutineScope,
    private val dispatchers: AppDispatchers,
    source: DataSource,
    session: SessionAcceptor,
    deviceIdentity: suspend () -> String,
    deviceName: suspend () -> String,
    routeAcceptedSession: suspend () -> Unit,
) {
    private val nameMutex = Mutex()
    private var cachedName: String? = null
    val router: SignInRouter =
        object : SignInRouter {
            override fun back() {
                navigator.navigate(Route.Welcome)
            }

            override fun forgot() {
                navigator.navigate(Route.Forgot)
            }

            override fun signUp() {
                navigator.navigate(Route.SignUp)
            }

            override fun guest() {
                navigator.navigate(Route.GuestWizard)
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
    val signIn: SignInModel by lazy {
        SignInModel(
            source,
            session,
            deviceIdentity = { withContext(dispatchers.io) { deviceIdentity() } },
            deviceName = {
                withContext(dispatchers.io) {
                    nameMutex.withLock {
                        cachedName
                            ?: deviceName().also { cachedName = it }
                    }
                }
            },
            accepted = router::acceptedSession,
            onTwoFactor = router::twoFactor,
        )
    }

    init {
        var previous = navigator.state.value.route
        appScope.launch(dispatchers.main, start = CoroutineStart.UNDISPATCHED) {
            navigator.state.collect { state ->
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
