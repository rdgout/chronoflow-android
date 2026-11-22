package net.chronoflow.app.auth

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.app.AppDispatchers
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.api.ApiPasskeyChallenge
import net.chronoflow.core.model.api.ApiPasskeyLoginInput
import net.chronoflow.core.model.api.ApiPasskeyRequestOptions
import net.chronoflow.core.model.api.PasswordSignInInput
import net.chronoflow.core.model.api.SessionResult
import net.chronoflow.core.model.api.SignInResult
import net.chronoflow.core.model.api.UserVerification
import net.chronoflow.core.model.auth.AuthenticationMethod
import net.chronoflow.core.model.passkey.PasskeyCapability
import net.chronoflow.core.model.passkey.PasskeyFailure
import net.chronoflow.core.navigation.RootNavigator
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.sync.session.sessionResult
import net.chronoflow.core.testing.sync.ScriptedSource
import net.chronoflow.feature.auth.model.AuthBannerKind
import net.chronoflow.feature.auth.model.SecretField
import net.chronoflow.feature.auth.model.SessionAcceptor
import net.chronoflow.feature.auth.model.passkey.DeterministicAssertionProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.coroutines.ContinuationInterceptor
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(Enclosed::class)
class AuthServicesTest {
    @RunWith(Parameterized::class)
    class Navigation(
        private val action: String,
        private val expected: Route,
    ) {
        @Test fun routerNavigatesWithoutPuttingASecretInTheRoute() =
            runTest(timeout = 60.seconds) {
                val dispatcher = StandardTestDispatcher(testScheduler)
                val navigator = RootNavigator().apply { navigate(Route.SignIn) }
                val services =
                    AuthServices(
                        navigator,
                        backgroundScope,
                        AppDispatchers(
                            dispatcher,
                            dispatcher,
                            dispatcher,
                        ),
                        ScriptedSource(),
                        SessionAcceptor {
                        },
                        { "id" },
                        { "test" },
                        { _, _ -> },
                    )
                services.signIn.navigation.twoFactor("artificial-challenge")
                when (action) {
                    "back" -> services.router.back()
                    "forgot" -> services.router.forgot()
                    "signUp" -> services.router.signUp()
                    "guest" -> services.router.guest()
                    "twoFactor" -> services.router.twoFactor()
                }
                runCurrent()
                assertEquals(expected, navigator.state.value.route)
                assertFalse(
                    navigator.state.value.route
                        .toString()
                        .contains("artificial-challenge"),
                )
                assertFalse(
                    navigator.state.value.route
                        .toString()
                        .contains("artificial-password"),
                )
            }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun cases(): List<Array<Any>> =
                listOf(
                    arrayOf("back", Route.Welcome),
                    arrayOf("forgot", Route.Forgot),
                    arrayOf("signUp", Route.SignUp),
                    arrayOf("guest", Route.GuestWizard),
                    arrayOf("twoFactor", Route.TwoFactor),
                )
        }
    }

    class Ownership {
        @Test fun unavailablePasskeyShowsTheServerBannerWithoutAcceptingASession() =
            runTest(timeout = 60.seconds) {
                val dispatcher = StandardTestDispatcher(testScheduler)
                var accepted = 0
                var assertions = 0
                var logins = 0
                val source =
                    object : DataSource by ScriptedSource() {
                        override suspend fun passkeyOptions() =
                            ApiPasskeyChallenge(
                                "challenge",
                                ApiPasskeyRequestOptions(
                                    "YQ",
                                    "app.chronoflow.net",
                                    60_000,
                                    UserVerification.Required,
                                    emptyList(),
                                ),
                            )

                        override suspend fun passkeyLogin(input: ApiPasskeyLoginInput): SessionResult {
                            logins++
                            return sessionResult()
                        }
                    }
                val services =
                    AuthServices(
                        RootNavigator(),
                        backgroundScope,
                        AppDispatchers(dispatcher, dispatcher, dispatcher),
                        source,
                        SessionAcceptor { accepted++ },
                        { "0199a3c0-0000-7000-8000-000000000001" },
                        { "test" },
                        { _, _ -> error("unavailable passkey routed") },
                        passkeyCapability = PasskeyCapability(true),
                        assertionProvider =
                            DeterministicAssertionProvider {
                                assertions++
                                throw PasskeyFailure.Unavailable
                            },
                    )
                services.signIn.editEmail("demo@example.com")
                services.signIn.submitPasskey(ServiceSecret())
                assertEquals(AuthBannerKind.Server, services.signIn.form.state.value.banner)
                assertEquals(0, accepted)
                assertEquals(1, assertions)
                assertEquals(0, logins)
            }

        @Test fun backToSignInOnlyLeavesTwoFactor() =
            runTest(timeout = 60.seconds) {
                val dispatcher = StandardTestDispatcher(testScheduler)
                val navigator = RootNavigator().apply { navigate(Route.TwoFactor) }
                val services =
                    AuthServices(
                        navigator,
                        backgroundScope,
                        AppDispatchers(dispatcher, dispatcher, dispatcher),
                        ScriptedSource(),
                        SessionAcceptor {},
                        { "id" },
                        { "test" },
                        { _, _ -> },
                    )
                services.router.backToSignIn()
                assertEquals(Route.SignIn, navigator.state.value.route)
                val signIn = navigator.state.value
                services.router.backToSignIn()
                assertEquals(signIn, navigator.state.value)
                navigator.navigate(Route.Today)
                val today = navigator.state.value
                services.router.backToSignIn()
                assertEquals(today, navigator.state.value)
            }

        @Test fun leavingAuthClearsTheFormAndChallengeAndKeepsTheEmail() =
            runTest(timeout = 60.seconds) {
                val dispatcher = StandardTestDispatcher(testScheduler)
                val navigator = RootNavigator().apply { navigate(Route.SignIn) }
                val services =
                    AuthServices(
                        navigator,
                        backgroundScope,
                        AppDispatchers(
                            dispatcher,
                            dispatcher,
                            dispatcher,
                        ),
                        ScriptedSource(),
                        SessionAcceptor {
                        },
                        { "id" },
                        { "test" },
                        { _, _ -> },
                    )
                services.signIn.editEmail("retained@example.com")
                services.signIn.navigation.twoFactor("artificial-challenge")
                services.signIn.form.receive(DataFailure.Offline)
                navigator.navigate(Route.Today)
                runCurrent()
                assertEquals("retained@example.com", services.signIn.email.value)
                assertNull(services.signIn.navigation.challengeID)
                assertNull(services.signIn.form.state.value.failure)
            }

        @Test fun movingBetweenAuthRoutesKeepsTheFormAndChallenge() =
            runTest(timeout = 60.seconds) {
                val dispatcher = StandardTestDispatcher(testScheduler)
                val navigator = RootNavigator().apply { navigate(Route.SignIn) }
                val services =
                    AuthServices(
                        navigator,
                        backgroundScope,
                        AppDispatchers(
                            dispatcher,
                            dispatcher,
                            dispatcher,
                        ),
                        ScriptedSource(),
                        SessionAcceptor {
                        },
                        { "id" },
                        { "test" },
                        { _, _ -> },
                    )
                services.signIn.editEmail("retained@example.com")
                services.signIn.navigation.twoFactor("artificial-challenge")
                services.signIn.form.receive(DataFailure.Offline)
                navigator.navigate(Route.Forgot)
                runCurrent()
                assertEquals("retained@example.com", services.signIn.email.value)
                assertEquals("artificial-challenge", services.signIn.navigation.challengeID)
                assertEquals(DataFailure.Offline, services.signIn.form.state.value.failure)
            }

        @Test fun constructionDoesNoIoAndDeviceValuesAreReadOnIoOnlyWhenNeeded() =
            runTest(timeout = 60.seconds) {
                val main = StandardTestDispatcher(testScheduler, name = "auth-main")
                val io = StandardTestDispatcher(testScheduler, name = "auth-io")
                var sourceCalls = 0
                var identities = 0
                var names = 0
                var routed = 0
                val source =
                    object : DataSource by ScriptedSource() {
                        override suspend fun signIn(input: PasswordSignInInput): SignInResult =
                            SignInResult.Session(sessionResult())
                    }
                val services =
                    AuthServices(
                        RootNavigator(),
                        backgroundScope,
                        AppDispatchers(main, io, io),
                        LazyPublicDataSource {
                            sourceCalls++
                            source
                        },
                        SessionAcceptor {},
                        {
                            assertSame(io, currentCoroutineContext()[ContinuationInterceptor])
                            identities++
                            "0199a3c0-0000-7000-8000-000000000001"
                        },
                        {
                            assertSame(io, currentCoroutineContext()[ContinuationInterceptor])
                            names++
                            "test"
                        },
                        { result, method ->
                            assertEquals(sessionResult(), result)
                            assertEquals(AuthenticationMethod.Password, method)
                            routed++
                        },
                    )
                val model = services.signIn
                assertEquals(0, sourceCalls)
                assertEquals(0, identities)
                assertEquals(0, names)
                model.editEmail("demo@example.com")
                model.submitPassword(ServiceSecret()) {}
                model.submitPassword(ServiceSecret()) {}
                assertEquals(2, sourceCalls)
                assertEquals(2, identities)
                assertEquals(1, names)
                assertEquals(2, routed)
            }
    }
}

private class ServiceSecret : SecretField {
    private var value = "artificial-password"

    override fun read(): String = value

    override fun clear() {
        value = ""
    }

    override fun toString(): String = "ServiceSecret(<redacted>)"
}
