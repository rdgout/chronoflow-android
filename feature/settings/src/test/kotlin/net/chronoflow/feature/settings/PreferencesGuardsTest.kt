package net.chronoflow.feature.settings

import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.Guards
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.feature.settings.support.SettingsScreenTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

internal fun preferenceGuardCases(): List<Array<Any>> =
    listOf(
        DeviceProfiles.small,
        DeviceProfiles.pixel8,
        DeviceProfiles.foldInner,
        DeviceProfiles.landscapeSideNav,
        DeviceProfiles.largeFont,
        DeviceProfiles.small.copy(name = "minimum280", widthDp = 280),
    ).flatMap { profile ->
        AppLanguage.entries.flatMap { language -> listOf(false, true).map { arrayOf(profile, language, it) } }
    }

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PreferencesGuardsTest(
    profile: DeviceProfile,
    language: AppLanguage,
    dark: Boolean,
) : SettingsScreenTest(profile, language, dark) {
    @Test fun preferencesFitTheProfile() {
        show { SettingsBoardFixture("Preferences", wide = profile.widthDp >= 600, shell = true) }
        Guards.assertAll(
            compose,
            profile,
            headingTags = emptySet(),
            interactiveRootTag = "settings.preferences.screen",
        )
        if (profile.widthDp >= 600) {
            val bounds = compose.onNodeWithTag("settings.preferences.screen.content").fetchSemanticsNode().boundsInRoot
            assertEquals(560f * profile.density, bounds.width, 1f)
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}-dark={2}")
        fun cases() = preferenceGuardCases()
    }
}
