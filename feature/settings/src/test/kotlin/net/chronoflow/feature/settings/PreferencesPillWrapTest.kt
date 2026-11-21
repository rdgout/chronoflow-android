package net.chronoflow.feature.settings

import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.feature.settings.preferences.PreferencesScreen
import net.chronoflow.feature.settings.support.SettingsScreenTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PreferencesPillWrapTest(
    profile: DeviceProfile,
    private val language: AppLanguage,
) : SettingsScreenTest(profile, language) {
    @Test fun timeFormatPillsWrapWhenTheContentRequiresIt() {
        show { PreferencesScreen(SettingsPreferenceBoardStates.preferences(), {}) }
        val first = compose.onNodeWithTag("settings.preferences.timeFormat.24").fetchSemanticsNode().boundsInRoot
        val second = compose.onNodeWithTag("settings.preferences.timeFormat.12").fetchSemanticsNode().boundsInRoot
        if (profile.widthDp == 375 || language == AppLanguage.Nl) {
            assertTrue(second.top >= first.bottom)
            assertEquals(first.left, second.left, 1f)
        } else {
            assertEquals(first.top, second.top, 1f)
            assertTrue(second.left >= first.right)
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}")
        fun cases(): List<Array<Any>> = listOf(
            DeviceProfiles.pixel8.copy(name = "board375", widthDp = 375),
            DeviceProfiles.pixel8,
        ).flatMap { profile -> AppLanguage.entries.map { arrayOf(profile, it) } }
    }
}
