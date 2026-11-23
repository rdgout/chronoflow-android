package net.chronoflow.app.shell

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.chronoflow.app.ChronoFlowApp
import net.chronoflow.app.MainActivity
import net.chronoflow.app.ioViolationsForTest
import net.chronoflow.app.resetIoViolationsForTest
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.core.testing.MockTripTemplates
import net.chronoflow.core.testing.tiers.Smoke
import net.chronoflow.debug.DebugRefreshProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@Smoke
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class ShellJourneySmokeTest : ScreenTest() {
    @Test fun tripsRefreshNavigationReselectAndSystemBack() {
        val started = System.nanoTime()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext as ChronoFlowApp
        instrumentation.runOnMainSync { app.graph.resetIoViolationsForTest() }
        val activeId =
            runBlocking(Dispatchers.IO) {
                MockTripTemplates
                    .templates()
                    .first()
                    .card.id
            }
        val scenario =
            launch(
                mapOf(
                    "CF_AUTH_TEST_SIGNED_IN" to "demo",
                    "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
                    "CF_PLAN_TEST_ROUTE" to "trips",
                    "CF_MOCK_DELAY" to "0",
                ),
            )
        val device = UiDevice.getInstance(instrumentation)
        compose.waitUntilExactlyOneExists(hasTestTag("trip.$activeId"), 15_000)
        compose.onNodeWithTag("trips.screen").assertIsDisplayed()
        compose.onNodeWithTag("tab.trips").assertIsSelected()
        compose.onNodeWithTag("trips.list").performTouchInput { swipeDown() }
        compose.waitUntil(3_000) { DebugRefreshProbe.accepted.get() == 1 && DebugRefreshProbe.presented != "none" }
        compose.onNodeWithTag("refresh.feedback").assertExists()
        compose.onNodeWithTag("trip.$activeId").performClick()
        compose.onNodeWithTag("route.tripPage").assertIsDisplayed()
        compose.onNodeWithTag("app.tabs").assertIsDisplayed()
        device.pressBack()
        compose.waitUntilExactlyOneExists(hasTestTag("trips.screen"), 3_000)
        compose.onNodeWithTag("trip.$activeId").performClick()
        compose.onNodeWithTag("route.tripPage").assertIsDisplayed()
        compose.onNodeWithTag("tab.trips").performClick()
        compose.onNodeWithTag("trips.screen").assertIsDisplayed()
        compose.onNodeWithTag("tab.today").performClick()
        compose.onNodeWithTag("route.today").assertIsDisplayed()
        compose.onNodeWithTag("tab.trips").performClick()
        compose.onNodeWithTag("trips.screen").assertIsDisplayed()
        device.pressBack()
        compose.waitUntilExactlyOneExists(hasTestTag("route.today"), 3_000)
        lateinit var activity: MainActivity
        scenario.onActivity { activity = it }
        device.pressBack()
        compose.waitUntil(3_000) {
            var finishing = false
            instrumentation.runOnMainSync { finishing = activity.isFinishing }
            finishing
        }
        instrumentation.runOnMainSync { assertEquals(0, app.graph.ioViolationsForTest) }
        assertTrue(
            "journey took ${(System.nanoTime() - started) / 1_000_000} ms",
            System.nanoTime() - started < 10_000_000_000L,
        )
    }
}
