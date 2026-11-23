package net.chronoflow.app.trip

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.chronoflow.app.ChronoFlowApp
import net.chronoflow.app.ioViolationsForTest
import net.chronoflow.app.resetIoViolationsForTest
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.core.testing.MockTripTemplates
import net.chronoflow.core.testing.tiers.Smoke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

@Smoke
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class TripPageSummarySeedTest : ScreenTest() {
    @Test fun summarySeedAndNativeBackReturnToMyTrips() {
        val started = System.nanoTime()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext as ChronoFlowApp
        instrumentation.runOnMainSync { app.graph.resetIoViolationsForTest() }
        val id =
            runBlocking(Dispatchers.IO) {
                MockTripTemplates
                    .templates()
                    .first()
                    .card.id
            }
        launch(
            mapOf(
                "CF_AUTH_TEST_SIGNED_IN" to "demo",
                "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
                "CF_PLAN_TEST_ROUTE" to "trips",
                "CF_PLAN_TEST_POSITION" to "summary",
                "CF_PLAN_TEST_FREEZE_CLOCK" to "1",
                "CF_MOCK_DELAY" to "0",
            ),
        )
        compose.waitUntilExactlyOneExists(hasTestTag("trip.$id"), 15_000)
        compose.onNodeWithTag("trip.$id").performClick()
        compose.waitUntilExactlyOneExists(hasTestTag("trip.summary"), 5_000)
        val summary = compose.onNodeWithTag("trip.summary").getUnclippedBoundsInRoot()
        val scroll = compose.onNodeWithTag("trip.scroll").getUnclippedBoundsInRoot()
        assertTrue(abs(summary.top.value - scroll.top.value) <= 1f)
        compose.onNodeWithTag("trip.dayMenu").assertExists()
        compose.onNodeWithTag("trip.legend").assertIsNotEnabled()
        instrumentation.runOnMainSync { assertEquals(0, app.graph.ioViolationsForTest) }
        val device = UiDevice.getInstance(instrumentation)
        device.pressBack()
        compose.waitUntilExactlyOneExists(hasTestTag("trips.screen"), 5_000)
        compose.onNodeWithTag("trips.screen").assertIsDisplayed()
        instrumentation.runOnMainSync { assertEquals(0, app.graph.ioViolationsForTest) }
        assertTrue("trip journey exceeded 10 seconds", System.nanoTime() - started < 10_000_000_000L)
    }
}
