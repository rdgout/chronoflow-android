package net.chronoflow.feature.settings.security

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.SafeInsetsDp
import net.chronoflow.core.testing.compose.Guards
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.feature.settings.SettingsBoardFixture
import net.chronoflow.feature.settings.SettingsSecurityBoardStates
import net.chronoflow.feature.settings.support.SettingsScreenTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsSecurityGuardsTest(
    private val state: String,
    profile: DeviceProfile,
    language: AppLanguage,
    dark: Boolean,
) : SettingsScreenTest(profile, language, dark) {
    @Test fun screenFitsTheProfile() {
        show { SettingsBoardFixture(state, wide = profile.widthDp >= 600, nativeSheet = true) }
        if (state == "PromptForgot") compose.onNodeWithTag("settings.security.prompt.forgot").performClick()
        // A centred dialog has its own coordinate origin and no system bars inside that window.
        val guardedProfile =
            if (state.startsWith("Prompt") && profile.widthDp >= 600) {
                profile.copy(insets = SafeInsetsDp(0f, 0f, 0f, 0f))
            } else {
                profile
            }
        Guards.assertAll(
            compose,
            guardedProfile,
            headingTags = emptySet(),
            rootTag = "settings.capture.window",
            interactiveRootTag =
                when {
                    state == "PromptLocked" -> "settings.delete.prompt"
                    state == "PromptForgot" -> "settings.security.prompt.forgotAlert"
                    state.startsWith("Prompt") -> "settings.security.prompt"
                    else -> null
                },
        )
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}-{2}-{3}")
        fun cases(): List<Array<Any>> =
            SettingsSecurityBoardStates.states.flatMap { state ->
                listOf(
                    DeviceProfiles.small.copy(name = "minimum280", widthDp = 280, fontScale = 1.3f),
                    DeviceProfiles.small,
                    DeviceProfiles.pixel8,
                    DeviceProfiles.foldInner,
                    DeviceProfiles.landscapeSideNav,
                    DeviceProfiles.largeFont,
                ).flatMap { profile ->
                    AppLanguage.entries.flatMap { language ->
                        listOf(false, true).map { dark -> arrayOf<Any>(state, profile, language, dark) }
                    }
                }
            }
    }
}
