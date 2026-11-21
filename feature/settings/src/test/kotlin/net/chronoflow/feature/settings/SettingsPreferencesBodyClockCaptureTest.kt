package net.chronoflow.feature.settings

import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.BoardCapture
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
    private val language: AppLanguage,
    dark: Boolean,
) : SettingsScreenTest(DeviceProfiles.pixel8, language, dark) {
    @Test fun capture() {
        show { SettingsBoardFixture(state, shell = true) }
        BoardCapture.capture(compose, "${board}_pixel8_${language.tag}_${if (dark) "dark" else "light"}")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}-{2}-{3}")
        fun cases(): List<Array<Any>> {
            val preferences =
                AppLanguage.entries.flatMap { language ->
                    listOf(false, true).map { dark ->
                        arrayOf<Any>(
                            if (language ==
                                AppLanguage.Nl
                            ) {
                                "SetPreferencesNL"
                            } else {
                                "SetPreferences"
                            },
                            "Preferences",
                            language,
                            dark,
                        )
                    }
                }
            val body =
                listOf("bodyMissing", "bodyReady", "bodyAdjusted", "bodyExcluded").flatMap { state ->
                    val themes = if (state in setOf("bodyMissing", "bodyReady")) listOf(false, true) else listOf(false)
                    themes.flatMap { dark ->
                        listOf("intro", "about", "sleep", "caffeine", "light", "health").map { section ->
                            val board = if (dark) "DarkSetBodyClock" else "SetBodyClock"
                            arrayOf<Any>("${board}_$state-$section", "$state-$section", AppLanguage.En, dark)
                        }
                    }
                }
            return preferences + body
        }
    }
}
