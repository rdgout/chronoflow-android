package net.chronoflow.feature.onboarding.walkthrough

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.compose.Guards
import net.chronoflow.core.testing.compose.LayoutCase
import net.chronoflow.core.testing.compose.LayoutMatrix
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
class WalkthroughScreenGuardsTest(
    private val case: LayoutCase,
    private val page: WalkthroughModel.Page,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(case)).around(compose)

    @Test fun fixedControlsAndScrollableContentFit() {
        compose.setContent {
            ProfileHost(case.profile) {
                ChronoFlowTheme(reduceMotion = true) { WalkthroughScreen(WalkthroughUiState(page = page), {}) }
            }
        }
        Guards.assertAll(compose, case.profile, emptySet())
        val button = if (page == WalkthroughModel.Page.Progress) WalkthroughTags.DONE else WalkthroughTags.NEXT
        compose.onNodeWithTag(WalkthroughTags.SKIP).assertIsDisplayed()
        val skipTouch = compose.onNodeWithTag(WalkthroughTags.SKIP).fetchSemanticsNode().touchBoundsInRoot
        with(compose.density) { assertTrue(skipTouch.height >= 48.dp.toPx() - 1f) }
        compose.onNodeWithTag(button).assertIsDisplayed()
        val buttonTouch = compose.onNodeWithTag(button).fetchSemanticsNode().touchBoundsInRoot
        with(compose.density) { assertTrue(buttonTouch.height >= 48.dp.toPx() - 1f) }
        val bounds = compose.onNodeWithTag(button).fetchSemanticsNode().boundsInRoot
        with(compose.density) {
            assertTrue(bounds.bottom <= (case.profile.heightDp - case.profile.insets.bottom).dp.toPx() + 1f)
        }
        compose.onNodeWithTag(WalkthroughTags.TITLE).performScrollTo().assertIsDisplayed()
        if (case.profile.widthDp >= 600) compose.onNodeWithTag(WalkthroughTags.CONTENT).assertWidthIsEqualTo(560.dp)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}")
        fun cases() = LayoutMatrix.wide.flatMap { c -> WalkthroughModel.Page.entries.map { arrayOf<Any>(c, it) } }
    }
}
