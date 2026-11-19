package net.chronoflow.app.trip

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import net.chronoflow.app.ChronoFlowApp
import net.chronoflow.app.ioViolationsForTest
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.core.testing.tiers.Smoke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@Smoke
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class TripPageStepTest : ScreenTest() {
    @Test fun testTripBlockActionsAndListReopen() {
        val started = System.nanoTime()
        launch(
            mapOf(
                "CF_AUTH_TEST_ID" to "019a562b-8000-7000-8000-00000000006d",
                "CF_AUTH_TEST_SIGNED_IN" to "demo",
                "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
                "CF_PLAN_TEST_ROUTE" to "trip:0199a3c0-0000-7000-8000-000000000002",
                "CF_PLAN_TEST_FREEZE_CLOCK" to "1",
                "CF_MOCK_DELAY" to "0",
                "cf.settings.language" to "en",
                "cf.reduceMotion" to "1",
            ),
        )
        compose.waitUntilExactlyOneExists(hasTestTag("trip.block.$BLOCK"), 5_000)
        compose.onNodeWithTag("trip.block.$BLOCK").performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.waitUntilExactlyOneExists(hasTestTag("step.done"), 5_000)
        compose.onNodeWithTag("step.done").performScrollTo().performClick()
        compose.waitUntilExactlyOneExists(hasTestTag("step.undo"), 5_000)
        compose.onNodeWithTag("step.undo").performScrollTo().performClick()
        compose.waitUntilExactlyOneExists(hasTestTag("step.done"), 5_000)
        compose.onNodeWithTag("step.skip").performScrollTo().performClick()
        compose.waitUntilExactlyOneExists(hasTestTag("step.undo"), 5_000)
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        compose.waitUntilDoesNotExist(hasTestTag("step.sheet"), 5_000)
        compose.onNodeWithTag("trip.viewToggle").performClick()
        compose.waitUntilExactlyOneExists(hasTestTag("trip.mode.list"), 5_000)
        compose.onNodeWithTag("trip.block.$BLOCK").performScrollTo().performClick()
        compose.waitUntilExactlyOneExists(hasTestTag("step.undo"), 5_000)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext as ChronoFlowApp
        instrumentation.runOnMainSync { assertEquals(0, app.graph.ioViolationsForTest) }
        assertTrue("step journey exceeded 10 seconds", System.nanoTime() - started < 10_000_000_000L)
    }

    private companion object {
        const val BLOCK = "caffeine-caffeine_serving-29864460"
    }
}
