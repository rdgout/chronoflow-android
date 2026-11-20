package net.chronoflow.feature.onboarding.terms

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.api.AccountPolicies
import net.chronoflow.core.testing.compose.Guards
import net.chronoflow.core.testing.compose.LayoutCase
import net.chronoflow.core.testing.compose.LayoutMatrix
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
class TermsScreenGuardsTest(
    private val case: LayoutCase,
    private val failed: Boolean,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(case)).around(compose)

    @Test fun contentAndActionsFit() {
        compose.setContent {
            ProfileHost(case.profile) {
                ChronoFlowTheme(reduceMotion = true) {
                    TermsScreen(TermsUiState(policies = AccountPolicies("1", "1"), failed = failed), {})
                }
            }
        }
        if (!failed && case.profile.name == "pixel8") compose.onNodeWithTag(TermsTags.ACCEPT).assertIsDisplayed()
        Guards.assertAll(compose, case.profile, emptySet())
        compose
            .onNodeWithTag(TermsTags.ACCEPT)
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
        compose
            .onNodeWithTag(TermsTags.SIGN_OUT)
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
        if (case.profile.widthDp >= 600) compose.onNodeWithTag(TermsTags.CONTENT).assertWidthIsEqualTo(560.dp)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-failed={1}")
        fun cases() = LayoutMatrix.wide.flatMap { c -> listOf(false, true).map { arrayOf<Any>(c, it) } }
    }
}
