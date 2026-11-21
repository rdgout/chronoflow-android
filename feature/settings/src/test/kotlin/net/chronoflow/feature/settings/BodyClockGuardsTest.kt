package net.chronoflow.feature.settings

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.compose.Guards
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.feature.settings.support.SettingsScreenTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BodyClockGuardsTest(
    profile: DeviceProfile,
    language: AppLanguage,
    dark: Boolean,
) : SettingsScreenTest(profile, language, dark) {
    @Test fun everyItemFitsTheProfile() {
        show { SettingsBoardFixture("bodyMissing", wide = profile.widthDp >= 600, shell = true) }
        guardItem(0)
        guardItem(1)
        guardItem(2)
        guardItem(3)
        guardItem(4)
        guardItem(5)
        guardItem(6)
        Guards.assertInk(compose, profile)
        if (profile.widthDp >= 600) {
            val bounds = compose.onNodeWithTag("settings.bodyClock.screen.content").fetchSemanticsNode().boundsInRoot
            assertEquals(560f * profile.density, bounds.width, 1f)
        }
    }

    private fun guardItem(index: Int) {
        compose.onNodeWithTag("settings.bodyClock.screen.content").performScrollToIndex(index)
        Guards.assertTextFits(compose, profile, scrollIntoView = false)
        Guards.assertTouchTargets(compose, profile)
        Guards.assertInteractiveInSafeArea(
            compose,
            profile,
            scrollIntoView = false,
            rootTag = "settings.bodyClock.screen",
        )
        Guards.assertNoInteractiveOverlap(compose, rootTag = "settings.bodyClock.screen")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}-dark={2}")
        fun cases() = preferenceGuardCases()
    }
}
