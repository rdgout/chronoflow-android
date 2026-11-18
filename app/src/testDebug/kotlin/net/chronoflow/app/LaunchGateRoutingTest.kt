@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.app

import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.navigation.debugName
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.debug.RouteProbe
import net.chronoflow.debug.ScreenSessionFixture
import net.chronoflow.debug.TestSeed
import net.chronoflow.debug.TestSeeds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = SessionTestApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LaunchGateRoutingTest {
    @get:Rule val compose = createEmptyComposeRule()

    private suspend fun TestScope.launchSeed(
        extras: Map<String, String>,
        expected: Route,
        check: suspend (AppGraph) -> Unit = {
        },
    ) {
        val app = ApplicationProvider.getApplicationContext<SessionTestApplication>()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val seed =
            TestSeed.parse(
                mapOf(TestSeed.TEST_ID_KEY to UUID.randomUUID().toString(), TestSeed.PROFILE_KEY to "screen") + extras,
            )
        TestSeeds.apply(
            app,
            seed,
            Dispatchers.IO,
            sessionCipher = SoftwareSessionCipher(),
            dispatchers = AppDispatchers(dispatcher, Dispatchers.IO, dispatcher),
        )
        val graph = app.graph
        try {
            ScreenSessionFixture.route(graph)
            runCurrent()
            ActivityScenario.launch<MainActivity>(TestSeeds.forwardIntent(app, seed)).use {
                compose.onNodeWithTag("route.${expected.debugName()}").assertExists()
                assertEquals(expected, graph.rootNavigator.state.value.route)
                check(graph)
            }
        } finally {
            graph.close()
        }
    }

    @Test fun nativeSignedOutLaunchAndForegroundWithoutAnIntentDoNotRequestPermissionOrChangeNavigation() =
        runTest(timeout = 60.seconds) {
            launchSeed(emptyMap(), Route.Welcome) { graph ->
                val revision = graph.rootNavigator.state.value.revision
                graph.activation.setActive(true)
                graph.handlePendingLaunchIntents()
                runCurrent()
                assertEquals(Route.Welcome, graph.rootNavigator.state.value.route)
                assertEquals(revision, graph.rootNavigator.state.value.revision)
            }
        }

    @Test fun signedInWalkthroughSeenLaunchesToday() =
        runTest(timeout = 60.seconds) {
            launchSeed(mapOf("CF_AUTH_TEST_SIGNED_IN" to "demo", "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1"), Route.Today)
        }

    @Test fun unverifiedLaunchesVerify() =
        runTest(timeout = 60.seconds) {
            launchSeed(mapOf("CF_AUTH_TEST_SIGNED_IN" to "demo", "CF_AUTH_TEST_UNVERIFIED" to "1"), Route.Verify)
        }

    @Test fun pendingTermsLaunchesTerms() =
        runTest(timeout = 60.seconds) {
            launchSeed(mapOf("CF_AUTH_TEST_SIGNED_IN" to "demo", "CF_AUTH_TEST_TERMS_PENDING" to "1"), Route.Terms)
        }

    @Test fun unseenWalkthroughLaunchesWalkthrough() =
        runTest(timeout = 60.seconds) {
            launchSeed(mapOf("CF_AUTH_TEST_SIGNED_IN" to "demo"), Route.Walkthrough())
        }

    @Test fun currentUnauthorizedRoutesToSignInAndErasesAccountFiles() =
        runTest(timeout = 60.seconds) {
            launchSeed(
                mapOf("CF_AUTH_TEST_SIGNED_IN" to "demo", "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1"),
                Route.Today,
            ) { graph ->
                graph.controller.unauthorized(checkNotNull(graph.controller.activeSession()).epoch)
                runCurrent()
                compose.onNodeWithTag("route.signIn").assertExists()
                assertEquals(Route.SignIn, RouteProbe.state.route)
                withContext(graph.dispatchers.io) {
                    assertTrue(File(graph.storageRoot(), "ChronoFlowMock/Accounts").listFiles()?.isEmpty() != false)
                }
            }
        }
}
