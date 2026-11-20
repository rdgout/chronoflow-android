package net.chronoflow.app.wizard

import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry

internal fun <T> withWizardPhoneHourCycle(
    cycle: String,
    block: () -> T,
): T {
    val automation = InstrumentationRegistry.getInstrumentation().uiAutomation

    fun shell(command: String): String =
        ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).bufferedReader().use {
            it.readText().trim()
        }
    val old = shell("settings get system time_12_24")
    try {
        shell("settings put system time_12_24 $cycle")
        return block()
    } finally {
        shell(if (old == "null") "settings delete system time_12_24" else "settings put system time_12_24 $old")
    }
}

internal fun wizardPickerSeed(
    language: String,
    dark: Boolean = false,
    board: String = "WizardOutbound",
): Map<String, String> =
    mapOf(
        "CF_AUTH_TEST_SIGNED_IN" to "demo",
        "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
        "CF_MOCK_DELAY" to "0",
        "CF_WIZARD_BOARD_CAPTURE" to board,
        "cf.settings.language" to language,
        "cf.settings.theme" to if (dark) "dark" else "light",
    )
