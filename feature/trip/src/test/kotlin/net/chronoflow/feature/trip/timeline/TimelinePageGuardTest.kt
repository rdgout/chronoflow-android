package net.chronoflow.feature.trip.timeline

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
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
import org.robolectric.ParameterizedRobolectricTestRunner.Parameters
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The page has no wide layout before 6e, so it runs the phone matrix. Timeline targets overlap by design (hit frames
 * of at least 48 dp around small tiles), so the overlap guard exempts them (OQ-1d-16); the touch-target and
 * safe-area guards do not. Headings get their designed check in
 * 6b, which is why `assertAll` is not used here.
 */
@OptIn(ExperimentalTestApi::class)
@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class TimelinePageGuardTest(
    private val case: LayoutCase,
) {
    private val composeRule = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(case)).around(composeRule)

    @Test fun pageFitsTheProfile() {
        val host = TimelineTestHost(fixture = "S3")
        val blockTags =
            host.model.prepared.days
                .flatMap { day -> day.blocks.map { "trip.block.${it.id}" } }
                .toSet()
        composeRule.setContent {
            ProfileHost(case.profile) { TimelinePage(host.viewModel, onBlock = {}) }
        }
        composeRule.waitUntilExactlyOneExists(hasTestTag("trip.timeline.ready"), 10_000)
        Guards.assertTextFits(composeRule, case.profile)
        Guards.assertTouchTargets(composeRule, case.profile)
        Guards.assertInteractiveInSafeArea(composeRule, case.profile)
        Guards.assertNoInteractiveOverlap(composeRule, exemptTags = blockTags)
        Guards.assertInk(composeRule, case.profile)
    }

    companion object {
        @JvmStatic
        @Parameters(name = "{0}")
        fun cases(): Collection<Array<Any>> = LayoutMatrix.parameters(LayoutMatrix.phone)
    }
}
