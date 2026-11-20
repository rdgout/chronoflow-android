package net.chronoflow.app.wizard

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.chronoflow.app.ChronoFlowApp
import net.chronoflow.app.ioViolationsForTest
import net.chronoflow.app.resetIoViolationsForTest
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.core.testing.tiers.Smoke
import net.chronoflow.feature.wizard.times.PickerIs24Hour
import net.chronoflow.feature.wizard.times.PickerLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@Smoke
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class WizardSleepTimeSheetDeviceTest : ScreenTest() {
    @Test fun timeSheetKeepsTheAppLanguageAndPhoneHourCycle_nl12() =
        withWizardPhoneHourCycle("12") {
            checkSheet("nl", false)
        }

    @Test fun timeSheetKeepsTheAppLanguageAndPhoneHourCycle_en24() =
        withWizardPhoneHourCycle("24") {
            checkSheet("en", true)
        }

    private fun checkSheet(
        language: String,
        is24Hour: Boolean,
    ) {
        val started = System.nanoTime()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext as ChronoFlowApp
        instrumentation.runOnMainSync { app.graph.resetIoViolationsForTest() }
        launch(wizardPickerSeed(language, board = "WizardSleepMember"))
        compose.waitUntilExactlyOneExists(hasTestTag("wizard.sleep.habitual_bed"), 5_000)
        compose.onNodeWithTag("wizard.sleep.habitual_bed").performClick()
        compose.waitUntilExactlyOneExists(hasTestTag("wizard.time-sheet"), 5_000)
        compose
            .onNodeWithTag("wizard.time-sheet")
            .assert(SemanticsMatcher.expectValue(PickerIs24Hour, is24Hour))
            .assert(SemanticsMatcher.expectValue(PickerLanguage, language))
        compose.onNodeWithTag("time-sheet.cancel").performClick()
        compose.waitUntilDoesNotExist(hasTestTag("wizard.time-sheet"), 5_000)
        instrumentation.runOnMainSync { assertEquals(0, app.graph.ioViolationsForTest) }
        assertTrue(
            "time sheet check took ${(System.nanoTime() - started) / 1_000_000} ms",
            System.nanoTime() - started < 10_000_000_000L,
        )
    }
}
