@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.app.shell

import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.app.AppDispatchers
import net.chronoflow.app.AppGraph
import net.chronoflow.app.MainActivity
import net.chronoflow.app.SessionTestApplication
import net.chronoflow.app.SoftwareSessionCipher
import net.chronoflow.core.testing.MockTripTemplates
import net.chronoflow.debug.TestSeed
import net.chronoflow.debug.TestSeeds
import org.junit.After
import org.junit.Rule
import org.robolectric.Robolectric
import org.robolectric.android.controller.ActivityController
import org.robolectric.util.ReflectionHelpers
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

abstract class ShellActivityFixture {
    @get:Rule val compose = createEmptyComposeRule()
    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    protected lateinit var graph: AppGraph
    private var controller: ActivityController<MainActivity>? = null
    protected lateinit var activeTripId: String

    protected fun launch(planRoute: String? = "trips") {
        val app = ApplicationProvider.getApplicationContext<SessionTestApplication>()
        val values =
            mutableMapOf(
                TestSeed.TEST_ID_KEY to UUID.randomUUID().toString(),
                TestSeed.PROFILE_KEY to "screen",
                "CF_AUTH_TEST_SIGNED_IN" to "demo",
                "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
                "CF_MOCK_DELAY" to "0",
            )
        if (planRoute != null) values["CF_PLAN_TEST_ROUTE"] = planRoute
        val seed = TestSeed.parse(values)
        runTest(dispatcher, timeout = 60.seconds) {
            TestSeeds.apply(
                app,
                seed,
                Dispatchers.IO,
                sessionCipher = SoftwareSessionCipher(),
                dispatchers = AppDispatchers(dispatcher, Dispatchers.IO, dispatcher),
            )
            activeTripId =
                withContext(Dispatchers.IO) {
                    MockTripTemplates
                        .templates()
                        .first()
                        .card.id
                }
        }
        graph = app.graph
        compose.mainClock.autoAdvance = false
        val activity =
            Robolectric
                .buildActivity(MainActivity::class.java, TestSeeds.forwardIntent(app, seed))
                .create()
                .start()
                .resume()
                .visible()
        controller = activity
        ReflectionHelpers
            .callInstanceMethod<ArrayList<Runnable>?>(
                activity
                    .get()
                    .window.decorView.viewTreeObserver,
                "captureFrameCommitCallbacks",
            )?.forEach { it.run() }
        waitFor { graph.rootNavigator.state.value.route != null }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
    }

    protected fun waitFor(condition: () -> Boolean) {
        compose.waitUntil(5_000) {
            scheduler.runCurrent()
            compose.mainClock.advanceTimeByFrame()
            condition()
        }
        compose.waitForIdle()
    }

    protected fun loaded() {
        waitFor { graph.sessionGraphs.current.value != null }
        waitFor { compose.onNodeWithTag("trip.$activeTripId").isDisplayed() }
    }

    @After fun closeShell() {
        controller?.pause()?.stop()?.destroy()
        if (::graph.isInitialized) runTest(dispatcher, timeout = 60.seconds) { graph.close() }
    }
}
