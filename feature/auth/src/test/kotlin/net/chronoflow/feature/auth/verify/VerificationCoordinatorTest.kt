package net.chronoflow.feature.auth.verify

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import net.chronoflow.core.datamock.MockMail
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.api.AuthLanguage
import net.chronoflow.core.model.api.RegisterInput
import net.chronoflow.core.model.api.SessionResult
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.model.session.SessionSnapshot
import net.chronoflow.core.model.session.SessionUpdate
import net.chronoflow.core.model.session.StoredSession
import net.chronoflow.core.navigation.RootNavigator
import net.chronoflow.core.navigation.Route
import net.chronoflow.feature.auth.links.VerificationLinkInbox
import net.chronoflow.feature.auth.model.RegisteredVerification
import net.chronoflow.feature.auth.model.VerifyState
import net.chronoflow.feature.auth.model.registered
import net.chronoflow.feature.auth.testing.AUTH_DEVICE_ID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class VerificationCoordinatorTest {
    @Test fun secondAccountConsumesItsOwnMailAfterTheFirstAccountIsWiped() =
        runTest(timeout = 60.seconds) {
            val f = fixture()
            f.coordinator.onVerify(3)
            val first = checkNotNull(f.coordinator.model.value)
            val signOut = first.scope.launch { first.signOut() }
            runCurrent()
            signOut.join()
            assertFalse(first.isActive)
            assertNull(f.coordinator.model.value)
            assertEquals(1, f.port.wipes)
            val next =
                f.registered.source.signUp(
                    RegisterInput(
                        "Second",
                        "second@example.com",
                        "NewPassword123!",
                        "NewPassword123!",
                        AUTH_DEVICE_ID,
                        "test",
                        AuthLanguage.En,
                    ),
                )
            f.port.signIn(next)
            runCurrent()
            f.coordinator.onVerify(f.port.valueEpoch)
            val second = checkNotNull(f.coordinator.model.value)
            assertNotSame(first, second)
            second.openEmail { error("mock opened native mail") }
            assertEquals(Route.Walkthrough(), f.navigator.state.value.route)
            assertEquals(listOf(second.context.email), f.port.confirmedEmails)
            assertEquals("second@example.com", f.port.account!!.email)
        }

    @Test fun unverifiedDifferentEmailReturnsToSignIn() =
        runTest(timeout = 60.seconds) {
            val f = fixture()
            f.coordinator.onVerify(3)
            val expected = f.port.snapshot
            val model = checkNotNull(f.coordinator.model.value)
            val signOut = model.scope.launch { model.signOut() }
            runCurrent()
            signOut.join()
            assertEquals(listOf(expected), f.port.discarded)
            assertEquals(SessionPresence.SignedOut, f.port.valuePresence)
            assertEquals(1, f.port.wipes)
            assertEquals(Route.SignIn, f.navigator.state.value.route)
            assertNull(f.coordinator.model.value)
        }

    @Test fun tamperedLinkOpensFailedScreen() =
        runTest(timeout = 60.seconds) {
            val f = fixture()
            f.coordinator.receiveVerificationLink(f.registered.input.copy(signature = "0".repeat(64)))
            assertEquals(Route.Verify, f.navigator.state.value.route)
            assertEquals(
                VerifyState.Failed,
                f.coordinator.model.value!!
                    .state,
            )
        }

    @Test fun malformedLinkIsIgnoredAcrossAuthentication() =
        runTest(timeout = 60.seconds) {
            val f = fixture(signedIn = false)
            f.coordinator.receiveVerificationLink(null)
            assertNull(f.inbox.pending)
            assertNull(f.navigator.state.value.route)
            assertEquals(0, f.clears)
            f.port.signIn(f.registered.result)
            f.coordinator.onVerify(f.port.valueEpoch)
            assertEquals(
                VerifyState.Waiting,
                f.coordinator.model.value!!
                    .state,
            )
        }

    @Test fun signedOutLinkWaitsForTheFirstMatchingSignIn() =
        runTest(timeout = 60.seconds) {
            val f = fixture(signedIn = false)
            f.coordinator.receiveVerificationLink(f.registered.input)
            assertEquals(Route.SignIn, f.navigator.state.value.route)
            assertEquals(1, f.clears)
            assertEquals(f.registered.input, f.inbox.pending)
            f.port.signIn(f.registered.result)
            f.coordinator.prepareVerification()
            assertNull(f.inbox.pending)
            f.navigator.navigate(Route.Verify)
            f.coordinator.model.value!!
                .activate()
            assertEquals(Route.Walkthrough(), f.navigator.state.value.route)
        }

    @Test fun verificationRequiredNavigatesOnlyForTheCurrentUnverifiedEpoch() =
        runTest(timeout = 60.seconds) {
            val f = fixture()
            f.coordinator.onVerify(2)
            assertNull(f.navigator.state.value.route)
            f.port.account = f.port.account!!.copy(hasVerifiedEmail = true)
            f.coordinator.onVerify(3)
            assertNull(f.navigator.state.value.route)
            f.port.account = f.port.account!!.copy(hasVerifiedEmail = false)
            f.coordinator.onVerify(3)
            assertEquals(Route.Verify, f.navigator.state.value.route)
            val state = f.navigator.state.value
            f.coordinator.onVerify(3)
            assertEquals(state, f.navigator.state.value)
        }

    @Test fun completionRoutesOnceEvenWhenTheGateRouterMovedFirst() = completion(true)

    @Test fun completionRoutesOnceEvenWhenTheGateRouterMovedFirstStillOnVerify() = completion(false)

    private fun completion(moved: Boolean) =
        runTest(timeout = 60.seconds) {
            val f = fixture()
            f.coordinator.onVerify(3)
            val model = f.coordinator.model.value!!
            f.port.onConfirm = { if (moved) f.navigator.navigate(Route.Walkthrough()) }
            val before = f.navigator.state.value.revision
            model.receiveLink(f.registered.input)
            model.check()
            assertEquals(Route.Walkthrough(), f.navigator.state.value.route)
            assertEquals(before + 1, f.navigator.state.value.revision)
            model.activate()
            assertEquals(before + 1, f.navigator.state.value.revision)
            assertEquals(1, f.port.confirmedEmails.size)
        }

    @Test fun staleBackCannotDiscardAReplacementAccount() =
        runTest(timeout = 60.seconds) {
            val f = fixture()
            f.coordinator.prepareVerification()
            val model = checkNotNull(f.coordinator.model.value)
            f.port.signIn(f.registered.result)
            model.signOut()
            assertTrue(f.port.discarded.isEmpty())
            assertEquals(0, f.port.wipes)
        }

    private suspend fun TestScope.fixture(signedIn: Boolean = true): CoordinatorFixture {
        val registered = registered()
        val port = FakeVerificationPort(registered.source)
        if (signedIn) port.install(registered.result, 3) else port.valueEpoch = 1
        val navigator = RootNavigator()
        val inbox = VerificationLinkInbox()
        val f = CoordinatorFixture(registered, port, navigator, inbox)
        val dispatcher = StandardTestDispatcher(testScheduler)
        f.coordinator =
            VerificationCoordinator(
                port,
                inbox,
                navigator,
                routeAccepted = { navigate, _ -> navigate(Route.Walkthrough()) },
                clearSignInSecrets = { f.clears++ },
                verificationMail =
                    VerificationMail { context ->
                        registered.source
                            .inbox()
                            .filterIsInstance<MockMail.Verification>()
                            .lastOrNull { it.to == context.email && it.input.id == context.accountId }
                            ?.input
                    },
                nowSeconds = { registered.clock.seconds },
                scope = backgroundScope,
                main = dispatcher,
                io = dispatcher,
            )
        return f
    }
}

private class CoordinatorFixture(
    val registered: RegisteredVerification,
    val port: FakeVerificationPort,
    val navigator: RootNavigator,
    val inbox: VerificationLinkInbox,
) {
    lateinit var coordinator: VerificationCoordinator
    var clears = 0
}

private class FakeVerificationPort(
    private val dataSource: DataSource,
) : VerificationSessionPort {
    var valueEpoch = 0L
    var valuePresence: SessionPresence = SessionPresence.SignedOut
    var snapshot: SessionSnapshot? = null
    var account: StoredVerificationAccount? = null
    val events = MutableSharedFlow<SessionUpdate>(extraBufferCapacity = 8)
    val discarded = mutableListOf<SessionSnapshot>()
    val confirmedEmails = mutableListOf<String>()
    var onConfirm: () -> Unit = {}
    var wipes = 0

    fun install(
        result: SessionResult,
        epoch: Long,
    ) {
        valueEpoch = epoch
        valuePresence = SessionPresence.SignedIn(result.account.id)
        snapshot = SessionSnapshot(StoredSession(result.token, result.account.id), epoch)
        account = StoredVerificationAccount(result.account.id, result.account.email, result.account.hasVerifiedEmail)
    }

    fun signIn(result: SessionResult) {
        install(result, valueEpoch + 1)
        events.tryEmit(SessionUpdate(valuePresence, valueEpoch))
    }

    override suspend fun epoch() = valueEpoch

    override suspend fun presence() = valuePresence

    override suspend fun activeSession() = snapshot

    override suspend fun storedAccount() = account

    override suspend fun source(snapshot: SessionSnapshot) = dataSource

    override fun updates() = events

    override suspend fun confirm(
        expected: SessionSnapshot,
        email: String,
    ): Boolean {
        if (snapshot != expected || account?.email != email) return false
        confirmedEmails += email
        account = account!!.copy(hasVerifiedEmail = true)
        onConfirm()
        return true
    }

    override suspend fun signOutDiscarding(expected: SessionSnapshot) {
        if (snapshot != expected) return
        discarded += expected
        dataSource.signOut()
        snapshot = null
        account = null
        valuePresence = SessionPresence.SignedOut
        valueEpoch++
        events.emit(SessionUpdate(valuePresence, valueEpoch))
        yield()
        wipes++
    }
}
