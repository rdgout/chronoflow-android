package net.chronoflow.app.auth

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import net.chronoflow.app.AppDispatchers
import net.chronoflow.app.AppGraph
import net.chronoflow.app.MainActivity
import net.chronoflow.app.SessionTestApplication
import net.chronoflow.app.SoftwareSessionCipher
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.debug.TestSeed
import net.chronoflow.debug.TestSeeds
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.util.ReflectionHelpers
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], application = SessionTestApplication::class)
class TwoFactorRoutingTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private lateinit var graph: AppGraph
    private var scenario: ActivityScenario<MainActivity>? = null

    private fun launch() {
        val app = ApplicationProvider.getApplicationContext<SessionTestApplication>()
        val seed =
            TestSeed.parse(
                mapOf(
                    TestSeed.TEST_ID_KEY to UUID.randomUUID().toString(),
                    TestSeed.PROFILE_KEY to "screen",
                    "CF_AUTH_TEST_AUDIT_SCREEN" to "two-factor",
                    "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
                    "CF_MOCK_DELAY" to "0",
                ),
            )
        runTest(dispatcher, timeout = 60.seconds) {
            TestSeeds.apply(
                app,
                seed,
                Dispatchers.IO,
                sessionCipher = SoftwareSessionCipher(),
                dispatchers = AppDispatchers(dispatcher, Dispatchers.IO, dispatcher),
            )
        }
        graph = app.graph
        compose.mainClock.autoAdvance = false
        scenario = ActivityScenario.launch<MainActivity>(TestSeeds.forwardIntent(app, seed))
        commitFrame()
        waitFor { graph.rootNavigator.state.value.route == Route.TwoFactor }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
    }

    private fun commitFrame() {
        scenario!!.onActivity { activity ->
            ReflectionHelpers
                .callInstanceMethod<ArrayList<Runnable>?>(
                    activity.window.decorView.viewTreeObserver,
                    "captureFrameCommitCallbacks",
                )?.forEach { it.run() }
        }
    }

    private fun waitFor(condition: () -> Boolean) {
        compose.waitUntil(5_000) {
            scheduler.runCurrent()
            compose.mainClock.advanceTimeByFrame()
            condition()
        }
        compose.waitForIdle()
    }

    @Test fun systemBackDiscardsChallenge() {
        launch()
        scenario!!.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitFor { graph.rootNavigator.state.value.route == Route.SignIn }
        assertNull(graph.auth.signIn.navigation.challengeID)
        assertEquals("twofactor@example.com", graph.auth.signIn.email.value)
    }

    @Test fun recreationKeepsTheScreenTheChallengeAndTheCode() {
        launch()
        val challenge = graph.auth.signIn.navigation.challengeID
        compose.onNodeWithTag("two-factor.code").performTextInput("123")
        waitFor {
            graph.auth.signIn.twoFactor.value
                ?.code == "123"
        }
        scenario!!.recreate()
        assertEquals(
            "model code survives activity recreation",
            "123",
            graph.auth.signIn.twoFactor.value
                ?.code,
        )
        assertEquals("challenge survives activity recreation", challenge, graph.auth.signIn.navigation.challengeID)
        commitFrame()
        waitFor { graph.rootNavigator.state.value.route == Route.TwoFactor }
        assertEquals(
            "model code survives first frame",
            "123",
            graph.auth.signIn.twoFactor.value
                ?.code,
        )
        assertEquals(
            "123",
            compose
                .onNodeWithTag("two-factor.code")
                .fetchSemanticsNode()
                .config[SemanticsProperties.EditableText]
                .text,
        )
        assertEquals(challenge, graph.auth.signIn.navigation.challengeID)
    }

    @After fun close() {
        scenario?.close()
        if (::graph.isInitialized) runTest(dispatcher, timeout = 60.seconds) { graph.close() }
    }
}
