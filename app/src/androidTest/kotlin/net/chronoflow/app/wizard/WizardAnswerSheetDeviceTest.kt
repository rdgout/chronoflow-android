package net.chronoflow.app.wizard

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.core.testing.tiers.Smoke
import net.chronoflow.feature.wizard.times.PickerIs24Hour
import net.chronoflow.feature.wizard.times.PickerLanguage
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@Smoke
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class WizardAnswerSheetDeviceTest : ScreenTest() {
    @Test fun ratingFieldAndDoneStayAboveTheKeyboard() {
        val started = System.nanoTime()
        launch(wizardPickerSeed("en", board = "WizardSheetBlue"))
        compose.waitUntilExactlyOneExists(hasTestTag("wizard.answer.rating"), 5_000)
        compose.onNodeWithTag("wizard.answer.rating").performClick()
        compose.assertWizardKeyboardGeometry(
            focusedWizardWindow(),
            queryTag = "wizard.answer.rating",
            resultTag = "wizard.answer.done",
            sheetTag = "wizard.answer.blue_blockers",
        )
        waitForImeToSettle("wizard.answer.cancel", focusedWizardWindow())
        compose.onNodeWithTag("wizard.answer.cancel").performClick()
        compose.waitUntilDoesNotExist(hasTestTag("wizard.answer.rating"), 5_000)
        assertWithinBudget(started)
    }

    @Test fun inlineWakePickerFollowsThePhoneHourCycle() =
        withWizardPhoneHourCycle("12") {
            val started = System.nanoTime()
            launch(wizardPickerSeed("nl", board = "WizardSheetWake"))
            compose.waitUntilExactlyOneExists(hasTestTag("wizard.answer.latest_wake"), 5_000)
            compose
                .onNodeWithTag("wizard.answer.latest_wake")
                .assert(SemanticsMatcher.expectValue(PickerIs24Hour, false))
                .assert(SemanticsMatcher.expectValue(PickerLanguage, "nl"))
            assertWithinBudget(started)
        }

    private fun assertWithinBudget(started: Long) {
        val elapsed = System.nanoTime() - started
        assertTrue("answer sheet check took ${elapsed / 1_000_000} ms", elapsed < 10_000_000_000L)
    }
}
