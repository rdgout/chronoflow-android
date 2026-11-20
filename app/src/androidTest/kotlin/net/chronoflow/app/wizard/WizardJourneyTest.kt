@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.app.wizard

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import net.chronoflow.app.ChronoFlowApp
import net.chronoflow.app.ioViolationsForTest
import net.chronoflow.app.resetIoViolationsForTest
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.core.testing.tiers.Smoke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@Smoke
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class WizardJourneyTest : ScreenTest() {
    @Test fun newTripRouteReachesTheNextStep() {
        val started = System.nanoTime()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext as ChronoFlowApp
        instrumentation.runOnMainSync { app.graph.resetIoViolationsForTest() }
        launch(
            mapOf(
                "CF_AUTH_TEST_SIGNED_IN" to "demo",
                "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
                "CF_MOCK_DELAY" to "0",
                "CF_PLAN_TEST_ROUTE" to "trips",
            ),
        )
        compose.waitUntilExactlyOneExists(hasTestTag("trips.create"), 15_000)
        compose.onNodeWithTag("trips.create").performClick()
        compose.waitUntilExactlyOneExists(hasTestTag("wizard.title"), 5_000)
        compose.onNodeWithTag("wizard.from").performClick()
        compose.waitUntilExactlyOneExists(hasTestTag("wizard.airport.query"), 5_000)
        compose.onNodeWithTag("wizard.airport.query").performTextReplacement("Amsterdam")
        compose.waitUntilExactlyOneExists(hasTestTag("wizard.airport.AMS"), 15_000)
        waitForImeToSettle("wizard.airport.AMS", focusedWizardWindow())
        compose.onNodeWithTag("wizard.airport.AMS").performClick()
        compose.waitUntilDoesNotExist(hasTestTag("wizard.airport-search"), 5_000)
        compose.onNodeWithTag("wizard.to").performClick()
        compose.waitUntilExactlyOneExists(hasTestTag("wizard.airport.query"), 5_000)
        compose.onNodeWithTag("wizard.airport.query").performTextReplacement("Dubai")
        compose.waitUntilExactlyOneExists(hasTestTag("wizard.airport.DXB"), 15_000)
        waitForImeToSettle("wizard.airport.DXB", focusedWizardWindow())
        compose.onNodeWithTag("wizard.airport.DXB").performClick()
        compose.waitUntilDoesNotExist(hasTestTag("wizard.airport-search"), 5_000)
        compose.onNodeWithTag("wizard.where.continue").assertIsEnabled().performClick()
        compose.waitUntilExactlyOneExists(hasTestTag("wizard.seam.outbound"), 5_000)
        UiDevice.getInstance(instrumentation).pressBack()
        compose.waitUntilExactlyOneExists(hasTestTag("wizard.route"), 5_000)
        compose.onNodeWithTag("wizard.close").performClick()
        compose.waitUntilExactlyOneExists(hasTestTag("cf.dialog.confirm"), 5_000)
        compose.onNodeWithTag("cf.dialog.confirm").performClick()
        compose.waitUntilExactlyOneExists(hasTestTag("trips.create"), 5_000)
        instrumentation.runOnMainSync { assertEquals(0, app.graph.ioViolationsForTest) }
        assertTrue(
            "wizard journey took ${(System.nanoTime() - started) / 1_000_000} ms",
            System.nanoTime() - started < 10_000_000_000L,
        )
    }

    @Test fun oneWayReviewSavesAndOpensTheTripPage() {
        val started = System.nanoTime()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext as ChronoFlowApp
        instrumentation.runOnMainSync { app.graph.resetIoViolationsForTest() }
        launch(
            mapOf(
                "CF_AUTH_TEST_SIGNED_IN" to "demo",
                "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
                "CF_MOCK_DELAY" to "0",
                "CF_WIZARD_BOARD_CAPTURE" to "WizardSaveJourney",
            ),
        )
        compose.waitUntilExactlyOneExists(hasTestTag("wizard.choice.one-way"), 5_000)
        compose.onNodeWithTag("wizard.choice.one-way").performClick()
        compose.waitUntilExactlyOneExists(hasTestTag("wizard.fine-tune.continue") and isEnabled(), 5_000)
        compose.waitUntil(5_000) { compose.onNodeWithTag("wizard.fine-tune.continue").isDisplayed() }
        compose.onNodeWithTag("wizard.fine-tune.continue").performClick()
        compose.waitUntilExactlyOneExists(hasTestTag("wizard.ready.continue") and isEnabled(), 3_000)
        compose.waitUntil(3_000) { compose.onNodeWithTag("wizard.ready.continue").isDisplayed() }
        compose.onNodeWithTag("wizard.ready.continue").performClick()
        compose.waitUntilExactlyOneExists(hasTestTag("trip.page"), 5_000)
        compose.onNodeWithTag("wizard.ready").assertDoesNotExist()
        compose.onNodeWithTag("wizard.calculating").assertDoesNotExist()
        compose.onNodeWithTag("tab.trips").assertIsSelected()
        instrumentation.runOnMainSync { assertEquals(0, app.graph.ioViolationsForTest) }
        assertTrue(
            "wizard save journey took ${(System.nanoTime() - started) / 1_000_000} ms",
            System.nanoTime() - started < 10_000_000_000L,
        )
    }
}
