package net.chronoflow.feature.settings

import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.BoardCapture
import net.chronoflow.core.testing.compose.captureWindow
import net.chronoflow.core.testing.tiers.Capture
import net.chronoflow.feature.settings.support.SettingsScreenTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Capture
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsCaptureTest(
    private val board: String,
    private val state: String,
    private val language: AppLanguage,
    dark: Boolean,
) : SettingsScreenTest(DeviceProfiles.pixel8, language, dark) {
    @Test fun capture() {
        show { SettingsBoardFixture(state, nativeSheet = true, shell = true) }
        val name = "${board}_pixel8_${language.tag}_${if (dark) "dark" else "light"}"
        if (state == "PendingSheet") {
            BoardCapture.captureWindow(compose, name, "settings.capture.window")
        } else {
            BoardCapture.capture(compose, name)
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}-{2}-{3}")
        fun cases(): List<Array<Any>> =
            listOf(
                arrayOf("SetHome", "Home", AppLanguage.En, false),
                arrayOf("SetHomeNL", "Home", AppLanguage.Nl, false),
                arrayOf("DarkSetHome", "Home", AppLanguage.En, true),
                arrayOf("SignOutPendingSettings", "PendingSheet", AppLanguage.En, false),
                arrayOf("SetAccount", "Account", AppLanguage.En, false),
                arrayOf("SetProfile", "Profile", AppLanguage.En, false),
                arrayOf("SetLanguage", "LanguageSystem", AppLanguage.En, false),
                arrayOf("SetStates", "SyncStates", AppLanguage.En, false),
                arrayOf("SetStatesMore", "StoredStates", AppLanguage.En, false),
            )
    }
}
