package net.chronoflow.app.guest

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import net.chronoflow.app.ChronoFlowApp
import net.chronoflow.app.ioViolationsForTest
import net.chronoflow.app.resetIoViolationsForTest
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.core.testing.tiers.Smoke
import net.chronoflow.debug.GuestWizardDebugSecrets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@Smoke
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class GuestWizardReviewIoTest : ScreenTest() {
    @Test fun firstGuestReviewFromSleepDoesNoMainThreadIo() {
        val started = System.nanoTime()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext as ChronoFlowApp
        instrumentation.runOnMainSync { app.graph.resetIoViolationsForTest() }
        launch(
            mapOf(
                "CF_MOCK_DELAY" to "0",
                "cf.settings.language" to null,
                "cf.settings.theme" to null,
                "CF_GUEST_WIZARD" to "1",
                "CF_WIZARD_BOARD_CAPTURE" to "WizardGuestJourney",
            ),
        )
        compose.waitUntilExactlyOneExists(hasTestTag("wizard.sleep.continue") and isEnabled(), 5_000)
        compose.onNodeWithTag("wizard.sleep.continue").performClick()
        compose.waitUntil(5_000) { compose.onNodeWithTag("wizard.choice.question.18_64").isDisplayed() }
        compose.waitUntil(5_000) {
            runBlocking {
                val graph = app.graph
                GuestWizardDebugSecrets.hasReviewSecret(
                    graph.guestWizardStore,
                    graph.guestSecrets,
                    graph.dispatchers.io,
                )
            }
        }
        instrumentation.runOnMainSync { assertEquals(0, app.graph.ioViolationsForTest) }
        assertTrue("guest review screen exceeded 10 s", System.nanoTime() - started < 10_000_000_000L)
    }
}
