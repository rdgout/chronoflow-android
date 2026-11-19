package net.chronoflow.feature.auth.twofactor

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.Guards
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import org.junit.Assert.assertEquals
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
class TwoFactorGuardsTest(
    private val state: String,
    private val profile: DeviceProfile,
    private val language: AppLanguage,
    private val dark: Boolean,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(profile, language, dark)).around(compose)

    @Test fun stateFitsTheWindowAndTextCap() {
        compose.setContent { ProfileHost(profile) { ChronoFlowTheme(darkTheme = dark) { TwoFactorSample(state) } } }
        Guards.assertAll(compose, profile, headingTags = setOf("auth.heading"))
        compose.onAllNodesWithTag("two-factor.submit").assertCountEquals(1)
        compose.onAllNodesWithTag("two-factor.toggle").assertCountEquals(1)
        if (profile.widthDp >= 600) {
            val card = compose.onNodeWithTag("auth.card").fetchSemanticsNode().boundsInRoot
            assertEquals(560f, card.width / profile.density, 0.5f)
            val left = profile.insets.left + (profile.widthDp - profile.insets.left - profile.insets.right - 560f) / 2
            assertEquals(left, card.left / profile.density, 0.5f)
        }
        if (state == "recovery-none") {
            compose.onNodeWithTag("two-factor.recovery_code.placeholder", useUnmergedTree = true).assertExists()
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}_{2}_{3}")
        fun cases(): List<Array<Any>> {
            val profiles =
                listOf(
                    DeviceProfiles.small to AppLanguage.Nl,
                    DeviceProfiles.pixel8 to AppLanguage.En,
                    DeviceProfiles.foldInner to AppLanguage.En,
                    DeviceProfiles.largeFont to AppLanguage.Nl,
                )
            return twoFactorStates.flatMap { state ->
                profiles.map { (profile, language) ->
                    arrayOf<Any>(state, profile, language, false)
                }
            } +
                listOf("code-attempts", "recovery-attempts").map {
                    arrayOf<Any>(it, DeviceProfiles.landscapeSideNav, AppLanguage.En, false)
                } +
                listOf("code-none", "code-server", "recovery-none", "recovery-fieldError").map {
                    arrayOf<Any>(it, DeviceProfiles.pixel8, AppLanguage.En, true)
                }
        }
    }
}
