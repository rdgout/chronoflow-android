package net.chronoflow.feature.settings

import net.chronoflow.core.model.config.AppLanguage
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
class SettingsMinimumWidthTest(
    private val state: String,
) : SettingsScreenTest(
        DeviceProfiles.largeFont.copy(name = "minimum", widthDp = 280),
        AppLanguage.Nl,
    ) {
    @Test fun minimumWidthFitsWithDutchAndTheRootFontCap() {
        show { SettingsBoardFixture(state) }
        Guards.assertAll(
            compose,
            profile,
            headingTags = emptySet(),
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
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-minimum-nl")
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
            ).map { arrayOf<Any>(it) }
    }
}
