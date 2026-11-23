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
import net.chronoflow.feature.wizard.times.PickerMinimumDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@Smoke
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class WizardFlightPickerDeviceTest : ScreenTest() {
    @Test fun nativePickerKeepsTheAppLanguageAndPhoneHourCycle_nl12() =
        withWizardPhoneHourCycle("12") {
            val started = System.nanoTime()
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val app = instrumentation.targetContext.applicationContext as ChronoFlowApp
            instrumentation.runOnMainSync { app.graph.resetIoViolationsForTest() }
            launch(wizardPickerSeed("nl"))
            compose.waitUntilExactlyOneExists(hasTestTag("wizard.0.departure.time"), 5_000)
            compose.onNodeWithTag("wizard.0.departure.time").performClick()
            compose.waitUntilExactlyOneExists(hasTestTag("wizard.time-dialog"), 5_000)
            compose
                .onNodeWithTag(
                    "wizard.time-dialog",
                ).assert(
                    SemanticsMatcher.expectValue(PickerIs24Hour, false),
                ).assert(SemanticsMatcher.expectValue(PickerLanguage, "nl"))
            compose.onNodeWithTag("wizard.picker.cancel").performClick()
            compose.waitUntilDoesNotExist(hasTestTag("wizard.time-dialog"), 5_000)
            compose.onNodeWithTag("wizard.0.arrival.date").performClick()
            compose.waitUntilExactlyOneExists(hasTestTag("wizard.date-dialog"), 5_000)
            compose
                .onNodeWithTag(
                    "wizard.date-dialog",
                ).assert(
                    SemanticsMatcher.expectValue(PickerLanguage, "nl"),
                ).assert(SemanticsMatcher.expectValue(PickerMinimumDate, "2026-10-12"))
            compose.onNodeWithTag("wizard.picker.done").performClick()
            compose.waitUntilDoesNotExist(hasTestTag("wizard.date-dialog"), 5_000)
            instrumentation.runOnMainSync { assertEquals(0, app.graph.ioViolationsForTest) }
            assertTrue(
                "picker check took ${(System.nanoTime() - started) / 1_000_000} ms",
                System.nanoTime() - started < 10_000_000_000L,
            )
        }

    @Test fun nativePickerKeepsTheAppLanguageAndPhoneHourCycle_en24() =
        withWizardPhoneHourCycle("24") {
            val started = System.nanoTime()
            launch(wizardPickerSeed("en"))
            compose.waitUntilExactlyOneExists(hasTestTag("wizard.0.departure.time"), 5_000)
            compose.onNodeWithTag("wizard.0.departure.time").performClick()
            compose.waitUntilExactlyOneExists(hasTestTag("wizard.time-dialog"), 5_000)
            compose
                .onNodeWithTag(
                    "wizard.time-dialog",
                ).assert(
                    SemanticsMatcher.expectValue(PickerIs24Hour, true),
                ).assert(SemanticsMatcher.expectValue(PickerLanguage, "en"))
            compose.onNodeWithTag("wizard.picker.cancel").performClick()
            compose.waitUntilDoesNotExist(hasTestTag("wizard.time-dialog"), 5_000)
            compose.onNodeWithTag("wizard.0.departure.date").performClick()
            compose.waitUntilExactlyOneExists(hasTestTag("wizard.date-dialog"), 5_000)
            compose
                .onNodeWithTag(
                    "wizard.date-dialog",
                ).assert(
                    SemanticsMatcher.expectValue(PickerLanguage, "en"),
                ).assert(SemanticsMatcher.keyNotDefined(PickerMinimumDate))
            compose.onNodeWithTag("wizard.picker.cancel").performClick()
            compose.waitUntilDoesNotExist(hasTestTag("wizard.date-dialog"), 5_000)
            assertTrue(
                "picker check took ${(System.nanoTime() - started) / 1_000_000} ms",
                System.nanoTime() - started < 10_000_000_000L,
            )
        }
}
