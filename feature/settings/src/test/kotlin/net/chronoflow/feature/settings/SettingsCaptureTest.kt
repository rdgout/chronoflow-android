package net.chronoflow.feature.settings

import net.chronoflow.core.testing.compose.BoardCapture
import net.chronoflow.core.testing.compose.BoardCaptureCell
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
    private val cell: BoardCaptureCell,
) : SettingsScreenTest(cell.profile, cell.language, cell.dark) {
    @Test fun capture() {
        show { SettingsBoardFixture(state, wide = profile.widthDp >= 600, nativeSheet = true, shell = true) }
        val name = "${board}_${cell.suffix}"
        captureSettingsBoard(compose, name, window = state == "PendingSheet")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}-{2}")
        fun cases(): List<Array<Any>> =
            listOf(
                "SetHome" to "Home",
                "SignOutPendingSettings" to "PendingSheet",
                "SetAccount" to "Account",
                "SetProfile" to "Profile",
                "SetLanguage" to "LanguageSystem",
                "SetStates" to "SyncStates",
                "SetStatesMore" to "StoredStates",
            ).flatMap { (board, state) ->
                BoardCaptureCell.standard.map { cell -> arrayOf<Any>(board, state, cell) }
            }
    }
}

internal fun captureSettingsBoard(
    compose: androidx.compose.ui.test.SemanticsNodeInteractionsProvider,
    name: String,
    window: Boolean,
) {
    if (window) {
        BoardCapture.captureWindow(compose, name, "settings.capture.window")
    } else {
        BoardCapture.capture(compose, name)
    }
}
