@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.app

import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.debug.TestSeed
import net.chronoflow.debug.TestSeeds
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.util.ReflectionHelpers
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

/** Android-only regression for seeded activity recreation; Robolectric frame commits are driven explicitly. */
@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = SessionTestApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SeededScreenRecreationTest {
    @get:Rule val compose = createEmptyComposeRule()

    private fun TestScope.commitAndDrain(
        scenario: ActivityScenario<MainActivity>,
        graph: AppGraph,
        before: Set<Job>,
    ) {
        scenario.onActivity { activity ->
            val callbacks =
                ReflectionHelpers.callInstanceMethod<ArrayList<Runnable>?>(
                    activity.window.decorView.viewTreeObserver,
                    "captureFrameCommitCallbacks",
                )
            callbacks?.forEach { it.run() }
        }
        val tasks =
            graph.appScope.coroutineContext.job.children
                .toSet() - before
        compose.waitUntil(timeoutMillis = 5_000) {
            runCurrent()
            tasks.all { it.isCompleted }
        }
    }

    @Test fun recreationKeepsTheRouteChosenAfterTheSeededScreen() =
        runTest(timeout = 60.seconds) {
            compose.mainClock.autoAdvance = false
            val app = ApplicationProvider.getApplicationContext<SessionTestApplication>()
            val dispatcher = StandardTestDispatcher(testScheduler)
            val seed =
                TestSeed.parse(
                    mapOf(
                        TestSeed.TEST_ID_KEY to UUID.randomUUID().toString(),
                        TestSeed.PROFILE_KEY to "screen",
                        "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
                    ),
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
                val initial =
                    graph.appScope.coroutineContext.job.children
                        .toSet()
                ActivityScenario.launch<MainActivity>(TestSeeds.forwardIntent(app, seed)).use { scenario ->
                    commitAndDrain(scenario, graph, initial)
                    assertEquals(Route.Welcome, graph.rootNavigator.state.value.route)
                    // Isolate activity first-frame routing from the initial gate observer's pending emission.
                    graph.gateRouter.stop()
                    runCurrent()
                    graph.rootNavigator.navigate(Route.SignIn)
                    val expected = graph.rootNavigator.state.value
                    val before =
                        graph.appScope.coroutineContext.job.children
                            .toSet()
                    scenario.recreate()
                    commitAndDrain(scenario, graph, before)
                    assertEquals(expected, graph.rootNavigator.state.value)
                }
            } finally {
                graph.closeAndJoinForTest()
            }
        }
}
