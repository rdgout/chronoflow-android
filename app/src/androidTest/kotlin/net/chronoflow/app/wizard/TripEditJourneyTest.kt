package net.chronoflow.app.wizard

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.waitUntilExactlyOneExists
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.chronoflow.app.ChronoFlowApp
import net.chronoflow.app.ioViolationsForTest
import net.chronoflow.app.resetIoViolationsForTest
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.core.testing.tiers.Smoke
import net.chronoflow.core.ui.plan.RefusedChangeDebug
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@Smoke
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class TripEditJourneyTest : ScreenTest() {
    @Test fun editFromActionsSavesAndOpensTheTripPage() {
        val started = System.nanoTime()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext as ChronoFlowApp
        instrumentation.runOnMainSync { app.graph.resetIoViolationsForTest() }
        RefusedChangeDebug.presentedCount.set(0)
        launch(
            mapOf(
                "CF_AUTH_TEST_SIGNED_IN" to "demo",
                "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
                "CF_MOCK_DELAY" to "0",
                "CF_PLAN_TEST_ROUTE" to "trip:0199a3c0-0000-7000-8000-000000000002",
                "CF_WIZARD_BOARD_CAPTURE" to "TripActions",
            ),
        )
        compose.waitUntilExactlyOneExists(hasTestTag("trip.actions.edit") and isEnabled(), 4_000)
        compose.onNodeWithTag("trip.actions.edit").performClick()
        compose.waitUntilExactlyOneExists(hasTestTag("wizard.ready"), 5_000)
        compose.onNodeWithTag("wizard.ready.continue").assertIsNotEnabled()
        compose.onNodeWithTag("wizard.ready.extras").performClick()
        compose.waitUntilExactlyOneExists(hasTestTag("wizard.preparation"), 4_000)
        val preparation = compose.onNodeWithTag("wizard.preparation")
        val current = preparation.fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current
        preparation.performSemanticsAction(SemanticsActions.SetProgress) { set ->
            assertTrue(set(((current.toInt() + 1) % 5).toFloat()))
        }
        compose.onNodeWithTag("wizard.fine-tune.continue").performClick()
        compose.waitUntilExactlyOneExists(hasTestTag("wizard.ready.continue") and isEnabled(), 5_000)
        compose.waitUntil(5_000) { compose.onNodeWithTag("wizard.ready.continue").isDisplayed() }
        compose.onNodeWithTag("wizard.ready.continue").performClick()
        compose.waitUntilExactlyOneExists(hasTestTag("trip.page"), 5_000)
        compose.onNodeWithTag("wizard.ready").assertDoesNotExist()
        compose.onNodeWithTag("wizard.calculating").assertDoesNotExist()
        compose.onNodeWithTag("tab.trips").assertIsSelected()
        instrumentation.runOnMainSync { assertEquals(0, app.graph.ioViolationsForTest) }
        assertEquals(0, RefusedChangeDebug.presentedCount.get())
        assertTrue(
            "edit journey took ${(System.nanoTime() - started) / 1_000_000} ms",
            System.nanoTime() - started < 10_000_000_000L,
        )
    }
}
