package net.chronoflow.app.links

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.onNodeWithTag
import kotlinx.coroutines.test.runTest
import net.chronoflow.app.SessionTestApplication
import net.chronoflow.app.auth.AuthResetActivityFixture
import net.chronoflow.core.model.auth.ResetPasswordLink
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.testing.AuthLinkUrls
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], application = SessionTestApplication::class)
class LinkRecreationTest : AuthResetActivityFixture() {
    private val link = ResetPasswordLink("cold-link-secret", "demo@example.com")

    @Test fun coldViewOpensResetAndRecreationDoesNotReplayIt() {
        launch(link = AuthLinkUrls.reset(link.token, link.email))
        waitFor { graph.rootNavigator.state.value.route == Route.Reset(link.email) }
        assertEquals(link, graph.auth.resetModel!!.link)
        onActivity { assertNull(it.intent.data) }
        onActivity { graph.rootNavigator.navigate(Route.SignIn) }
        waitFor { graph.auth.resetModel == null }
        recreate(Route.SignIn)
        compose.onNodeWithTag("sign-in.email").assertExists()
        assertNull(graph.resetRouter.pending)
    }

    @Test fun historyRelaunchDoesNotCaptureTheLink() {
        launch(
            link = AuthLinkUrls.reset(link.token, link.email),
            flags = Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY,
            initialRoute = Route.SignIn,
        )
        assertEquals(Route.SignIn, graph.rootNavigator.state.value.route)
        assertNull(graph.pendingLaunchIntents.consume())
        assertNull(graph.auth.resetModel)
        onActivity { assertNull(it.intent.data) }
    }

    @Test fun warmViewKeepsTheLaunchIntentAndCapturesTheLinkOnce() {
        launch("sign-in")
        val before = graph.rootNavigator.state.value.revision
        onActivity { activity ->
            val original = activity.intent
            val incoming =
                Intent(Intent.ACTION_VIEW, Uri.parse(AuthLinkUrls.reset(link.token, link.email)))
                    .addCategory(Intent.CATEGORY_BROWSABLE)
            ReflectionHelpers.callInstanceMethod<Unit>(
                activity,
                "onNewIntent",
                ClassParameter.from(Intent::class.java, incoming),
            )
            assertSame(original, activity.intent)
            assertNull(activity.intent.data)
        }
        waitFor { graph.rootNavigator.state.value.route == Route.Reset(link.email) }
        assertEquals(link, graph.auth.resetModel!!.link)
        assertEquals(before + 2, graph.rootNavigator.state.value.revision)
        assertNull(graph.pendingLaunchIntents.consume())
        onActivity { graph.authLinks.deliver() }
        recreate(Route.Reset(link.email))
        assertEquals(before + 2, graph.rootNavigator.state.value.revision)
        assertNull(graph.resetRouter.pending)
    }

    @Test fun debugSeedCapturesInMemoryAndDoesNotForwardTheSecret() {
        launch(extras = mapOf("CF_AUTH_TEST_LINK" to AuthLinkUrls.reset(link.token, link.email)))
        waitFor { graph.rootNavigator.state.value.route == Route.Reset(link.email) }
        assertEquals(link, graph.auth.resetModel!!.link)
        onActivity { assertFalse(it.intent.hasExtra("CF_AUTH_TEST_LINK")) }
    }

    @Test fun signedInLinkOpensOnceAfterTheRealSignOutCompletes() {
        launch(extras = mapOf("CF_AUTH_TEST_SIGNED_IN" to "demo"), link = AuthLinkUrls.reset(link.token, link.email))
        waitFor { graph.resetRouter.pending == link }
        assertEquals(Route.Today, graph.rootNavigator.state.value.route)
        runTest(
            dispatcher,
            timeout = 60.seconds,
        ) { graph.controller.signOut(discardPending = true, unauthorized = true) }
        waitFor { graph.rootNavigator.state.value.route == Route.Reset(link.email) }
        assertEquals(SessionPresence.SignedOut, graph.controller.presence)
        assertEquals(link, graph.auth.resetModel!!.link)
        assertNull(graph.resetRouter.pending)
        val revision = graph.rootNavigator.state.value.revision
        recreate(Route.Reset(link.email))
        assertEquals(revision, graph.rootNavigator.state.value.revision)
    }
}
