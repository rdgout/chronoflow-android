package net.chronoflow.feature.settings

import net.chronoflow.core.testing.compose.BoardCapture
import net.chronoflow.core.testing.compose.BoardCaptureCell
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
class SettingsPreferencesBodyClockCaptureTest(
    private val board: String,
    private val state: String,
    private val cell: BoardCaptureCell,
) : SettingsScreenTest(cell.profile, cell.language, cell.dark) {
    @Test fun capture() {
        show { SettingsBoardFixture(state, wide = profile.widthDp >= 600, shell = true) }
        BoardCapture.capture(compose, "${board}_${cell.suffix}")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}-{2}")
        fun cases(): List<Array<Any>> {
            val preferences =
                BoardCaptureCell.standard.map { cell ->
                    arrayOf<Any>("SetPreferences", "Preferences", cell)
                }
            val body =
                listOf("bodyMissing", "bodyReady", "bodyAdjusted", "bodyExcluded").flatMap { state ->
                    listOf("intro", "about", "sleep", "caffeine", "light", "health").flatMap { section ->
                        BoardCaptureCell.standard.map { cell ->
                            arrayOf<Any>("SetBodyClock_$state-$section", "$state-$section", cell)
                        }
                    }
                }
            return preferences + body
        }
    }
}
