package net.chronoflow.app.guest

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.core.testing.tiers.Smoke
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@Smoke
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class GuestRecoveryScreenTest : ScreenTest() {
    @Test fun testWelcomeHandsOverToRecoveredSleep() {
        val started = System.nanoTime()
        launch(mapOf("CF_GUEST_RECOVERY_BOARD" to "WizardSleep"))
        compose.waitUntilExactlyOneExists(hasTestTag("wizard.sleep.continue"), 5_000)
        compose.waitUntil(5_000) { compose.onNodeWithTag("wizard.sleep.continue").isDisplayed() }
        compose.onNodeWithTag("wizard.sleep.habitual_bed").assertExists()
        compose.onNodeWithTag("welcome.plan").assertDoesNotExist()
        compose.onNodeWithTag("launch.opening").assertDoesNotExist()
        assertTrue("guest recovery exceeded 10 s", System.nanoTime() - started < 10_000_000_000L)
    }
}
