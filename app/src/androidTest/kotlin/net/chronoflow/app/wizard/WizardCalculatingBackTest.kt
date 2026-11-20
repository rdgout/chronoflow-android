package net.chronoflow.app.wizard

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.core.testing.tiers.Smoke
import org.junit.Test
import org.junit.runner.RunWith

@Smoke
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class WizardCalculatingBackTest : ScreenTest() {
    @Test fun systemBackKeepsCalculating() {
        launch(
            mapOf(
                "CF_AUTH_TEST_SIGNED_IN" to "demo",
                "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
                "CF_MOCK_DELAY" to "0",
                "CF_WIZARD_BOARD_CAPTURE" to "WizardCalculating",
            ),
        )
        compose.waitUntilExactlyOneExists(hasTestTag("wizard.calculating"), 5_000)
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.pressBack()
        device.pressBack()
        compose.onNodeWithTag("wizard.calculating").assertExists()
        compose.onNodeWithTag("wizard.close").assertDoesNotExist()
    }
}
