package net.chronoflow.app.auth

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.api.PasswordSignInInput
import net.chronoflow.core.model.api.SignInResult
import net.chronoflow.core.model.auth.AuthenticationMethod
import net.chronoflow.core.model.launch.Gate
import net.chronoflow.core.model.passkey.PasskeyCapability
import net.chronoflow.core.model.session.StoredAccountReadiness
import net.chronoflow.core.model.session.readStoredSession
import net.chronoflow.core.navigation.RootNavigator
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.navigation.toRoute
import net.chronoflow.core.sync.session.AccountReadinessSource
import net.chronoflow.core.sync.session.MemorySessionStore
import net.chronoflow.core.sync.session.RecordingAccountStore
import net.chronoflow.core.sync.session.SessionController
import net.chronoflow.core.sync.session.SessionTeardown
import net.chronoflow.core.sync.session.StoredDataReadiness
import net.chronoflow.core.sync.session.sessionResult
import net.chronoflow.core.testing.sync.ScriptedSource
import net.chronoflow.core.testing.sync.TestGate
import net.chronoflow.feature.auth.model.SessionAcceptor
import net.chronoflow.feature.auth.model.SignInModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(Enclosed::class)
class SignInModelRoutingTests {
    @RunWith(Parameterized::class)
    class Committed(
        private val storedGate: Gate,
        private val cancelAfterCommit: Boolean,
        private val method: AuthenticationMethod,
    ) {
        @Test fun committedSessionUsesStoredGateWithAndWithoutCancellation() =
            runTest(timeout = 60.seconds) {
                val dispatcher = StandardTestDispatcher(testScheduler)
                val sync = TestGate()
                val credentials = MemorySessionStore()
                val controller =
                    SessionController(credentials, RecordingAccountStore(), SessionTeardown(sleep = {}), dispatcher)
                controller.attach(requestSync = { sync.block() })
                val readiness =
                    StoredDataReadiness(
                        controller,
                        AccountReadinessSource { id ->
                            flowOf(StoredAccountReadiness(id, storedGate, false))
                        },
                        dispatcher,
                    )
                val navigator = RootNavigator()
                val routes = mutableListOf<Route>()
                val verificationEpochs = mutableListOf<Long>()
                val secret = RoutingSecret()
                val model =
                    model(controller, method, dispatcher, accepted = {
                        readiness.routeAcceptedSession(navigate = { gate ->
                            routes += gate.toRoute()
                            navigator.navigate(gate.toRoute())
                        }, verification = { epoch ->
                            verificationEpochs +=
                                epoch
                            ; routes += Route.Verify
                            navigator.navigate(Route.Verify)
                        })
                    })
                var request: Job? = null
                try {
                    request =
                        backgroundScope.launch {
                            if (method == AuthenticationMethod.Passkey) {
                                model.submitPasskey(secret)
                            } else {
                                model.submitPassword(secret) {}
                                if (method == AuthenticationMethod.TwoFactor) {
                                    assertNotNull(model.twoFactor.value)
                                    assertNull(controller.activeSession())
                                    assertNull(credentials.data)
                                    model.twoFactor.value!!.editCode("123456")
                                    model.twoFactor.value!!.submit()
                                    assertEquals("", model.twoFactor.value!!.code)
                                    assertNull(model.twoFactor.value!!.sessionFailure)
                                }
                            }
                        }
                    sync.waitUntilBlocked()
                    val stored = credentials.readStoredSession()
                    assertNotNull(stored)
                    assertEquals(stored, controller.activeSession()?.session)
                    assertTrue(routes.isEmpty())
                    assertTrue(
                        model.twoFactor.value
                            ?.form
                            ?.busy ?: model.form.busy,
                    )
                    if (cancelAfterCommit) request.cancel()
                    sync.release()
                    request.join()
                    assertEquals(listOf(storedGate.toRoute()), routes)
                    assertEquals(storedGate.toRoute(), navigator.state.value.route)
                    assertEquals(
                        if (storedGate ==
                            Gate.Verify
                        ) {
                            listOf(controller.epoch)
                        } else {
                            emptyList<Long>()
                        },
                        verificationEpochs,
                    )
                    assertEquals("", secret.read())
                    assertFalse(model.form.busy)
                    assertNull(model.form.state.value.banner)
                    assertNull(model.sessionFailure)
                    assertEquals(stored, controller.activeSession()?.session)
                } finally {
                    withContext(NonCancellable) {
                        sync.release()
                        request?.cancel()
                        request?.join()
                        readiness.close()
                        controller.close()
                    }
                }
            }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}-cancel={1}-{2}")
            fun cases(): List<Array<Any>> =
                Gate.entries.flatMap { gate ->
                    listOf(
                        false,
                        true,
                    ).flatMap { cancel ->
                        listOf(
                            AuthenticationMethod.Password,
                            AuthenticationMethod.Passkey,
                            AuthenticationMethod.TwoFactor,
                        ).map { method -> arrayOf(gate, cancel, method) }
                    }
                }
        }
    }

    @RunWith(Parameterized::class)
    class Stale(
        private val storedGate: Gate,
        private val cancelRequest: Boolean,
    ) {
        @Test fun signOutDuringStoredGateReadPreventsStaleRouting() =
            runTest(timeout = 60.seconds) {
                val dispatcher = StandardTestDispatcher(testScheduler)
                val read = TestGate()
                val controller =
                    SessionController(
                        MemorySessionStore(),
                        RecordingAccountStore(),
                        SessionTeardown(sleep = {}),
                        dispatcher,
                    )
                val readiness =
                    StoredDataReadiness(
                        controller,
                        AccountReadinessSource { id ->
                            read.block()
                            flowOf(StoredAccountReadiness(id, storedGate, false))
                        },
                        dispatcher,
                    )
                val navigator = RootNavigator()
                val routes = mutableListOf<Route>()
                val epochs = mutableListOf<Long>()
                val secret = RoutingSecret()
                val model =
                    model(controller, AuthenticationMethod.Password, dispatcher, accepted = {
                        readiness.routeAcceptedSession(navigate = {
                            routes +=
                                it.toRoute()
                            ; navigator.navigate(it.toRoute())
                        }, verification = {
                            epochs += it
                            routes += Route.Verify
                            navigator.navigate(Route.Verify)
                        })
                    })
                var request: Job? = null
                try {
                    request = backgroundScope.launch { model.submitPassword(secret) {} }
                    read.waitUntilBlocked()
                    assertNotNull(controller.activeSession())
                    if (cancelRequest) request.cancel()
                    controller.signOut(discardPending = true, unauthorized = true)
                    routes += Route.SignIn
                    navigator.navigate(Route.SignIn)
                    read.release()
                    request.join()
                    runCurrent()
                    assertEquals(listOf(Route.SignIn), routes)
                    assertEquals(Route.SignIn, navigator.state.value.route)
                    assertTrue(epochs.isEmpty())
                    assertEquals("", secret.read())
                    assertFalse(model.form.busy)
                    assertNull(model.form.state.value.banner)
                } finally {
                    withContext(NonCancellable) {
                        read.release()
                        request?.cancel()
                        request?.join()
                        readiness.close()
                        controller.close()
                    }
                }
            }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}-cancel={1}")
            fun cases(): List<Array<Any>> =
                listOf(
                    Gate.Verify,
                    Gate.Terms,
                ).flatMap { gate -> listOf(false, true).map { cancel -> arrayOf(gate, cancel) } }
        }
    }
}

private fun model(
    controller: SessionController,
    method: AuthenticationMethod,
    io: kotlinx.coroutines.CoroutineDispatcher,
    accepted: suspend () -> Unit,
): SignInModel {
    val result = sessionResult()
    val source =
        if (method ==
            AuthenticationMethod.TwoFactor
        ) {
            twoFactorTestSource(io, password = "artificial-password")
        } else {
            object : DataSource by ScriptedSource() {
                override suspend fun signIn(input: PasswordSignInInput): SignInResult = SignInResult.Session(result)
            }
        }
    return SignInModel(
        source,
        SessionAcceptor(controller::accept),
        { "0199a3c0-0000-7000-8000-000000000001" },
        { "test" },
        PasskeyCapability(method == AuthenticationMethod.Passkey),
        passkey = { result },
        accepted = {
            _,
            _,
            ->
            accepted()
        },
        onTwoFactor = {},
    ).also {
        it.editEmail(
            if (method ==
                AuthenticationMethod.TwoFactor
            ) {
                "twofactor@example.com"
            } else {
                "demo@example.com"
            },
        )
    }
}

private class RoutingSecret : net.chronoflow.feature.auth.model.SecretField {
    private var value = "artificial-password"

    override fun read(): String = value

    override fun clear() {
        value = ""
    }

    override fun toString(): String = "RoutingSecret(<redacted>)"
}
