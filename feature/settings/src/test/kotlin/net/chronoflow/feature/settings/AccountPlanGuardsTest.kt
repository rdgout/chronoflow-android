package net.chronoflow.feature.settings

import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.SafeInsetsDp
import net.chronoflow.core.testing.compose.Guards
import net.chronoflow.core.testing.tiers.Layout
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
class AccountPlanGuardsTest(
    private val state: String,
    profile: DeviceProfile,
    language: AppLanguage,
    dark: Boolean,
) : SettingsScreenTest(profile, language, dark) {
    @Test fun screenFitsTheProfile() {
        show { SettingsBoardFixture(state, wide = profile.widthDp >= 600, nativeSheet = true) }
        val prompt =
            when (state) {
                "PasskeysAdd", "PasskeysRemove" -> "settings.passkeys.prompt"
                "DeletePrompt", "DeleteWrong", "DeleteLocked" -> "settings.delete.prompt"
                "DeleteConfirmation" -> "settings.delete.screen"
                else -> null
            }
        val guarded =
            if (prompt != null && state != "DeleteConfirmation" && profile.widthDp >= 600) {
                profile.copy(insets = SafeInsetsDp(0f, 0f, 0f, 0f))
            } else {
                profile
            }
        Guards.assertAll(
            compose,
            guarded,
            rootTag = "settings.capture.window",
            interactiveRootTag = prompt,
            headingTags = emptySet(),
            inkChannelDifference = if (state.endsWith("Loading")) 2 else 16,
        )
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}-{2}-{3}")
        fun cases(): List<Array<Any>> =
            SettingsAccountPlanBoardStates.states.flatMap { state ->
                listOf(
                    DeviceProfiles.small,
                    DeviceProfiles.pixel8,
                    DeviceProfiles.foldInner,
                    DeviceProfiles.landscapeSideNav,
                    DeviceProfiles.largeFont,
                ).flatMap { profile ->
                    AppLanguage.entries.flatMap { language ->
                        listOf(false, true).map { dark ->
                            arrayOf<Any>(state, profile, language, dark)
                        }
                    }
                }
            }
    }
}
