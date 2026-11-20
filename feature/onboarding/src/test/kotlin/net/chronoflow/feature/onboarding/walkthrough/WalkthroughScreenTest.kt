package net.chronoflow.feature.onboarding.walkthrough

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WalkthroughScreenTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)

    private fun show(state: WalkthroughUiState? = WalkthroughUiState()) {
        compose.setContent {
            ProfileHost(DeviceProfiles.pixel8) { ChronoFlowTheme(reduceMotion = true) { WalkthroughScreen(state, {}) } }
        }
    }

    @Test fun titleIsAHeadingAndAPoliteLiveRegion() {
        show()
        compose
            .onNodeWithTag(WalkthroughTags.TITLE)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test fun previewIsHiddenFromAccessibility() {
        show(WalkthroughUiState(page = WalkthroughModel.Page.Categories))
        compose
            .onNodeWithTag(
                WalkthroughTags.PREVIEW,
                useUnmergedTree = true,
            ).assert(
                SemanticsMatcher("cleared semantics") {
                    it.config.isClearingSemantics
                },
            )
    }

    @Test fun dotsAreHiddenFromAccessibility() {
        show()
        compose.onNodeWithTag(WalkthroughTags.DOTS).assert(
            SemanticsMatcher("cleared semantics") { it.config.isClearingSemantics },
        )
    }

    @Test fun loadingShowsOnlyTheBackground() {
        show(null)
        compose.onNodeWithTag(WalkthroughTags.SKIP).assertDoesNotExist()
    }

    @Test fun completedProgressHasOnlyDoneAndDisabledControls() {
        show(WalkthroughUiState(page = WalkthroughModel.Page.Progress, completed = true))
        compose.onNodeWithTag(WalkthroughTags.DONE).assertExists().assertIsNotEnabled()
        compose.onNodeWithTag(WalkthroughTags.NEXT).assertDoesNotExist()
        compose.onNodeWithTag(WalkthroughTags.SKIP).assertIsNotEnabled()
    }
}
