package net.chronoflow.feature.settings

import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
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
class SettingsGuardsTest(
    private val state: String,
    profile: DeviceProfile,
    language: AppLanguage,
    dark: Boolean,
) : SettingsScreenTest(profile, language, dark) {
    @Test fun screenFitsTheProfile() {
        show { SettingsBoardFixture(state, wide = profile.widthDp >= 600) }
        val heading =
            when (state) {
                "Sheet" -> setOf("settings.pending.title")
                "AccountMissing" -> setOf("settings.missing.title")
                else -> emptySet()
            }
        Guards.assertAll(
            compose,
            profile,
            headingTags = heading,
            inkChannelDifference =
                if (state ==
                    "Loading"
                ) {
                    2
                } else {
                    16
                },
        )
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}-{2}-{3}")
        fun cases(): List<Array<Any>> =
            listOf(
                "Home",
                "HomeNoAccount",
                "HomePending",
                "HomeSyncing",
                "HomeOffline",
                "Sheet",
                "Account",
                "AccountMissing",
                "Loading",
                "Profile",
                "ProfileError",
                "ProfileSaved",
                "LanguageSystem",
                "LanguageEn",
                "LanguageNl",
            ).flatMap { state ->
                listOf(
                    DeviceProfiles.small,
                    DeviceProfiles.pixel8,
                    DeviceProfiles.foldInner,
                    DeviceProfiles.landscapeSideNav,
                    DeviceProfiles.largeFont,
                ).flatMap { profile ->
                    AppLanguage.entries.flatMap { language ->
                        listOf(false, true).map { arrayOf<Any>(state, profile, language, it) }
                    }
                }
            }
    }
}
