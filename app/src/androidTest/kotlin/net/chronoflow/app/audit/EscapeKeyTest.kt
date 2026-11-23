package net.chronoflow.app.audit

import android.view.KeyEvent
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.core.testing.tiers.Audit
import org.junit.Test

@Audit
@OptIn(ExperimentalTestApi::class)
class EscapeKeyTest : ScreenTest() {
    private val account =
        mapOf(
            "CF_AUTH_TEST_SIGNED_IN" to "demo",
            "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
            "CF_MOCK_DELAY" to "0",
            "CF_PLAN_TEST_FREEZE_CLOCK" to "1",
        )
    private val trip = account + ("CF_PLAN_TEST_ROUTE" to "trip:0199a3c0-0000-7000-8000-000000000001")

    @Test fun testEscapeClosesStepSheet() {
        launch(trip + ("CF_PLAN_TEST_SHEET" to "step:sleep-sleep-29865450"))
        escape("step.sheet")
    }

    @Test fun testEscapeClosesLegendSheet() {
        launch(trip + ("CF_PLAN_TEST_SHEET" to "legend"))
        escape("legend.sheet")
    }

    @Test fun testEscapeClosesPasswordPrompt() {
        launch(account + mapOf("CF_SETTINGS_ROUTE" to "twoFactor", "CF_AUTH_TEST_SIGNED_IN" to "twofactor"))
        compose.waitUntilExactlyOneExists(hasTestTag("settings.twoFactor.disable"), 5_000)
        compose.onNodeWithTag("settings.twoFactor.disable").performScrollTo().performClick()
        escape("settings.security.prompt.sheet")
    }

    @Test fun testEscapeClosesAlert() {
        launch(account + ("CF_WIZARD_BOARD_CAPTURE" to "WizardDiscard"))
        escape("cf.dialog.confirm")
    }

    private fun escape(tag: String) {
        compose.waitUntilExactlyOneExists(hasTestTag(tag), 5_000)
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.pressKeyCode(KeyEvent.KEYCODE_ESCAPE)
        compose.waitUntilDoesNotExist(hasTestTag(tag), 5_000)
    }
}
