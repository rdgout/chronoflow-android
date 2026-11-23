package net.chronoflow.app.guest

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.chronoflow.app.ChronoFlowApp
import net.chronoflow.app.ioViolationsForTest
import net.chronoflow.app.resetIoViolationsForTest
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.core.testing.tiers.Smoke
import net.chronoflow.debug.GuestWizardDebugCounters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@Smoke
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class GuestWizardJourneyTest : ScreenTest() {
    @Test fun testWelcomeThroughReadyRequestsSignUp() {
        val started = System.nanoTime()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext as ChronoFlowApp
        instrumentation.runOnMainSync { app.graph.resetIoViolationsForTest() }
        launch(
            mapOf(
                "CF_MOCK_DELAY" to "0",
                "cf.settings.language" to null,
                "cf.settings.theme" to null,
                "CF_GUEST_WIZARD_JOURNEY" to "1",
                "CF_WIZARD_BOARD_CAPTURE" to "WizardReadyGuest",
            ),
        )
        tap("welcome.plan")
        tap("wizard.ready.continue", 3_000)
        compose.waitUntil(3_000) { GuestWizardDebugCounters.signUpRequested.get() == 1 }
        assertEquals(1, GuestWizardDebugCounters.signUpRequested.get())
        compose.onNodeWithTag("wizard.ready.questions").assertExists()
        instrumentation.runOnMainSync { assertEquals(0, app.graph.ioViolationsForTest) }
        assertTrue("guest journey exceeded 10 s", System.nanoTime() - started < 10_000_000_000L)
    }

    private fun tap(
        tag: String,
        timeout: Long = 5_000,
    ) {
        compose.waitUntil(timeout) { compose.onNode(hasTestTag(tag) and isEnabled()).isDisplayed() }
        compose.onNodeWithTag(tag).performClick()
    }
}
