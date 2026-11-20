package net.chronoflow.app.wizard

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.core.testing.compose.BoardCapture
import net.chronoflow.core.testing.tiers.Capture
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@Capture
@OptIn(ExperimentalTestApi::class)
@RunWith(Parameterized::class)
class PickerDateTimeDeviceCaptureTest(
    private val name: String,
    private val time: Boolean,
    private val language: String,
    private val dark: Boolean,
    private val cycle: String,
) : ScreenTest() {
    @Test fun capturesTheNativeDialog() =
        withWizardPhoneHourCycle(cycle) {
            launch(wizardPickerSeed(language, dark))
            val pill = "wizard.0.departure." + if (time) "time" else "date"
            val dialog = if (time) "wizard.time-dialog" else "wizard.date-dialog"
            compose.waitUntilExactlyOneExists(hasTestTag(pill), 5_000)
            compose.onNodeWithTag(pill).performClick()
            compose.waitUntilExactlyOneExists(hasTestTag(dialog), 5_000)
            compose.waitForIdle()
            BoardCapture.capture(
                checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()),
                name,
            )
            compose.onNodeWithTag("wizard.picker.cancel").performClick()
            compose.waitUntilDoesNotExist(hasTestTag(dialog), 5_000)
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases() =
            listOf(
                arrayOf<Any>("PickerDateTime_date_en_light", false, "en", false, "24"),
                arrayOf<Any>("PickerDateTime_time24_en_light", true, "en", false, "24"),
                arrayOf<Any>("PickerDateTime_date_nl_dark", false, "nl", true, "12"),
                arrayOf<Any>("PickerDateTime_time12_nl_dark", true, "nl", true, "12"),
            )
    }
}
