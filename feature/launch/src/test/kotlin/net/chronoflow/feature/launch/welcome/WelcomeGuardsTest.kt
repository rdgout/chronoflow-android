package net.chronoflow.feature.launch.welcome

import androidx.compose.ui.test.junit4.v2.createComposeRule
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.Guards
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class WelcomeGuardsTest(
    private val profile: DeviceProfile,
    private val language: AppLanguage,
    private val dark: Boolean,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(profile, language, dark)).around(compose)

    @Test fun welcomeFitsTheWindowAndSafeArea() {
        compose.setContent {
            ProfileHost(
                profile,
            ) { ChronoFlowTheme(darkTheme = dark) { WelcomeScreen(null, null, {}, {}) } }
        }
        Guards.assertAll(compose, profile, headingTags = setOf(WelcomeTags.HEADLINE))
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}-dark={2}")
        fun cases(): List<Array<Any>> =
            DeviceProfiles.all.flatMap { profile ->
                AppLanguage.entries.flatMap { language ->
                    listOf(false, true).map { dark -> arrayOf<Any>(profile, language, dark) }
                }
            }
    }
}
