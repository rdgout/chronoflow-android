package net.chronoflow.app.links

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.app.AppDispatchers
import net.chronoflow.app.PendingLaunchIntents
import net.chronoflow.app.auth.AuthServices
import net.chronoflow.core.model.api.SignedVerificationInput
import net.chronoflow.core.model.auth.ResetPasswordLink
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.model.session.SessionUpdate
import net.chronoflow.core.navigation.RootNavigator
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.testing.AuthLinkUrls
import net.chronoflow.core.testing.sync.ScriptedSource
import net.chronoflow.feature.auth.links.GuestResetLinkRouter
import net.chronoflow.feature.auth.links.VerifyLinkReceiver
import net.chronoflow.feature.auth.model.SessionAcceptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class AuthLinkRoutingTest {
    private val link = ResetPasswordLink("token", "demo@example.com")

    @Test fun coldSignedOutWaitsForAttachThenOpensSignInAndReset() =
        runTest(timeout = 60.seconds) {
            val f = Fixture(this, SessionPresence.SignedOut)
            f.capture(link)
            runCurrent()
            assertTrue(f.routes.isEmpty())
            f.dispatcher.attach()
            runCurrent()
            assertEquals(listOf(Route.SignIn, Route.Reset(link.email)), f.routes)
            assertEquals(link, f.auth.resetModel!!.link)
            assertEquals(0, f.sessionCalls)
        }

    @Test fun coldSignedInDefersUntilSignOutAndOpensOnce() =
        runTest(timeout = 60.seconds) {
            val f = Fixture(this, SessionPresence.SignedIn("account"))
            f.capture(link)
            f.dispatcher.attach()
            runCurrent()
            assertEquals(link, f.reset.pending)
            assertTrue(f.routes.isEmpty())
            f.signOut()
            runCurrent()
            assertEquals(listOf(Route.SignIn, Route.Reset(link.email)), f.routes)
            assertEquals(SessionPresence.SignedOut, f.presence)
            assertEquals(link, f.auth.resetModel!!.link)
            assertNull(f.reset.pending)
            f.dispatcher.deliver()
            f.signOut()
            runCurrent()
            assertEquals(2, f.routes.size)
        }

    @Test fun warmSignedOutOpensImmediately() =
        runTest(timeout = 60.seconds) {
            val f = Fixture(this, SessionPresence.SignedOut)
            f.dispatcher.attach()
            f.capture(link)
            runCurrent()
            assertEquals(Route.Reset(link.email), f.navigator.state.value.route)
        }

    @Test fun warmSignedInKeepsTheLinkPending() =
        runTest(timeout = 60.seconds) {
            val signedIn = SessionPresence.SignedIn("account")
            val f = Fixture(this, signedIn)
            f.dispatcher.attach()
            f.capture(link)
            runCurrent()
            assertEquals(link, f.reset.pending)
            assertEquals(signedIn, f.presence)
            assertEquals(0, f.sessionCalls)
            assertTrue(f.routes.isEmpty())
        }

    @Test fun twoLinksWhileSignedInOpenOnlyTheLatest() =
        runTest(timeout = 60.seconds) {
            val f = Fixture(this, SessionPresence.SignedIn("account"))
            f.dispatcher.attach()
            f.capture(link)
            runCurrent()
            val latest = ResetPasswordLink("latest", "other@example.com")
            f.capture(latest)
            runCurrent()
            f.signOut()
            runCurrent()
            assertEquals(latest, f.auth.resetModel!!.link)
            assertEquals(listOf(Route.SignIn, Route.Reset(latest.email)), f.routes)
        }

    @Test fun unknownPresenceWaitsForSignedOutRestore() =
        runTest(timeout = 60.seconds) {
            val f = Fixture(this, null)
            f.capture(link)
            f.dispatcher.attach()
            runCurrent()
            assertTrue(f.routes.isEmpty())
            assertEquals(link, f.reset.pending)
            f.signOut()
            runCurrent()
            assertEquals(Route.Reset(link.email), f.navigator.state.value.route)
        }

    @Test fun validVerificationGoesOnlyToTheVerificationReceiver() =
        runTest(timeout = 60.seconds) {
            val f = Fixture(this, SessionPresence.SignedOut)
            val input =
                SignedVerificationInput(
                    "0199a3c0-0000-7000-8000-000000000001",
                    "a".repeat(40),
                    1,
                    "b".repeat(64),
                )
            f.pending.captureLink(AuthLinkUrls.verify(input.id, input.hash, input.expires, input.signature))
            f.dispatcher.attach()
            runCurrent()
            assertEquals(listOf(input), f.verifications)
            assertNull(f.reset.pending)
            assertTrue(f.routes.isEmpty())
        }

    @Test fun malformedVerificationGoesOnlyToTheVerificationReceiver() =
        runTest(timeout = 60.seconds) {
            val f = Fixture(this, SessionPresence.SignedOut)
            f.pending.captureLink("https://app.chronoflow.net/email/verify/bad")
            f.dispatcher.attach()
            runCurrent()
            assertEquals(listOf<SignedVerificationInput?>(null), f.verifications)
            assertNull(f.reset.pending)
            assertTrue(f.routes.isEmpty())
        }
}

private class Fixture(
    scope: TestScope,
    initial: SessionPresence?,
) {
    var presence = initial
    private val updates = MutableStateFlow<SessionUpdate?>(null)
    val navigator = RootNavigator()
    val routes = mutableListOf<Route?>()
    var sessionCalls = 0
    private val dispatcherForTests = StandardTestDispatcher(scope.testScheduler)
    val auth =
        AuthServices(
            navigator,
            scope.backgroundScope,
            AppDispatchers(
                dispatcherForTests,
                dispatcherForTests,
                dispatcherForTests,
            ),
            ScriptedSource(),
            SessionAcceptor {
                sessionCalls++
            },
            { "id" },
            { "test" },
            { _, _ -> sessionCalls++ },
        )
    val reset =
        GuestResetLinkRouter({ presence }, updates, scope.backgroundScope) {
            navigator.navigate(Route.SignIn)
            routes += navigator.state.value.route
            auth.openReset(it)
            routes += navigator.state.value.route
        }
    val verifications = mutableListOf<SignedVerificationInput?>()
    val pending = PendingLaunchIntents()
    val dispatcher =
        AuthLinkDispatcher(
            pending,
            AuthLinkRouting(
                scope.backgroundScope,
                reset,
                VerifyLinkReceiver {
                    verifications +=
                        it
                },
            ),
        )

    fun capture(link: ResetPasswordLink) {
        pending.captureLink(AuthLinkUrls.reset(link.token, link.email))
        dispatcher.deliver()
    }

    fun signOut() {
        presence = SessionPresence.SignedOut
        updates.value =
            SessionUpdate(SessionPresence.SignedOut, (updates.value?.epoch ?: 0) + 1)
    }
}
