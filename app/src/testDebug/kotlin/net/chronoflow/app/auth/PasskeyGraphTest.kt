package net.chronoflow.app.auth

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
import net.chronoflow.feature.auth.model.AuthBannerKind
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
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
class PasskeyGraphTest {
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
                    "CF_AUTH_TEST_AUDIT_SCREEN" to "sign-in",
                    "cf.passkeyCapability" to "1",
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
        waitFor { graph.rootNavigator.state.value.route == Route.SignIn }
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

    @Test fun mockPasskeyShowsTheServerBannerWithoutOpeningSystemUi() {
        launch()
        assertTrue(graph.passkeyCapability.enabled)
        assertTrue(graph.auth.signIn.passkeyCapability.enabled)
        compose.onNodeWithTag("sign-in.passkey").performClick()
        waitFor { graph.auth.signIn.form.state.value.banner == AuthBannerKind.Server }
        compose.onNodeWithTag("auth.banner").assertExists()
        scenario!!.onActivity { assertNull(shadowOf(it).nextStartedActivity) }
    }

    @Test fun activityAnchorAttachesAndDetaches() {
        launch()
        scenario!!.onActivity { assertSame(it, graph.passkeyAnchor.current()) }
        scenario!!.close()
        scenario = null
        assertNull(graph.passkeyAnchor.current())
    }

    @After fun close() {
        scenario?.close()
        if (::graph.isInitialized) runTest(dispatcher, timeout = 60.seconds) { graph.close() }
    }
}
