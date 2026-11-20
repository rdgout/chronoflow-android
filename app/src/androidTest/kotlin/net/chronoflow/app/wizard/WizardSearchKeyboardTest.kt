@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.app.wizard

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.core.testing.tiers.Smoke
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@Smoke
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class WizardSearchKeyboardTest : ScreenTest() {
    @Test fun fieldAndFirstResultStayAboveTheKeyboard() {
        val started = System.nanoTime()
        launch(
            mapOf(
                "CF_AUTH_TEST_SIGNED_IN" to "demo",
                "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
                "CF_MOCK_DELAY" to "0",
                "CF_WIZARD_BOARD_CAPTURE" to "WizardStopSearch",
            ),
        )
        compose.waitUntilExactlyOneExists(hasTestTag("wizard.airport.DXB"), 15_000)
        compose.onNodeWithTag("wizard.airport.query").performClick()
        compose.assertWizardKeyboardGeometry(focusedWizardWindow())
        compose.onNodeWithTag("wizard.airport.cancel").performClick()
        compose.waitUntilDoesNotExist(hasTestTag("wizard.airport-search"), 5_000)
        assertTrue(
            "keyboard check took ${(System.nanoTime() - started) / 1_000_000} ms",
            System.nanoTime() - started < 10_000_000_000L,
        )
    }
}
