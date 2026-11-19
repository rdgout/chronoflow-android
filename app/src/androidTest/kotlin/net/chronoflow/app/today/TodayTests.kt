package net.chronoflow.app.today

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import net.chronoflow.app.ChronoFlowApp
import net.chronoflow.app.ioViolationsForTest
import net.chronoflow.app.resetIoViolationsForTest
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.app.testing.predictiveBack
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.testing.tiers.Smoke
import net.chronoflow.debug.RouteProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@Smoke
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class TodayTests : ScreenTest() {
    @Test fun testTodayOpensTheTripRouteAndReturnsToItsRows() {
        val started = System.nanoTime()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext as ChronoFlowApp
        instrumentation.runOnMainSync { app.graph.resetIoViolationsForTest() }
        val device = UiDevice.getInstance(instrumentation)
        val scenario =
            launch(
                mapOf(
                    "CF_UI_TEST_PROFILE" to "screen",
                    "CF_AUTH_TEST_SIGNED_IN" to "demo",
                    "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
                ),
            )
        compose.waitUntilExactlyOneExists(hasTestTag("today.trip"), 5_000)
        compose.onNodeWithTag("today.trip").performClick()
        compose.waitUntilExactlyOneExists(hasTestTag("trip.scroll"), 4_000)
        device.pressBack()
        compose.waitUntilExactlyOneExists(hasTestTag("today.screen"), 4_000)
        assertEquals(Route.Today, RouteProbe.state.route)
        compose.onNodeWithTag("today.trip").performClick()
        compose.waitUntilExactlyOneExists(hasTestTag("trip.scroll"), 4_000)
        device.predictiveBack()
        compose.waitUntilExactlyOneExists(hasTestTag("today.screen"), 4_000)
        assertEquals(Route.Today, RouteProbe.state.route)
        scenario.onActivity { activity ->
            assertEquals(0, (activity.application as ChronoFlowApp).graph.ioViolationsForTest)
        }
        val elapsed = System.nanoTime() - started
        assertTrue("Today journey took ${elapsed / 1_000_000} ms", elapsed < 10_000_000_000L)
    }
}
