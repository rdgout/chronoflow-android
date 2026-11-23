@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.feature.wizard

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import net.chronoflow.core.designsystem.haptics.HapticEvent
import net.chronoflow.core.designsystem.haptics.LocalHaptics
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.compose.RecordingHaptics
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.setRouteAirport
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class WhereSemanticsTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)
    private val events = mutableListOf<WizardEvent>()
    private val haptics = RecordingHaptics()

    private fun host(draft: MemberWizardDraft = routeDraft()) {
        compose.setContent {
            WizardTestHost {
                CompositionLocalProvider(
                    LocalHaptics provides haptics,
                ) { WizardScreen(screenState(draft), events::add) }
            }
        }
    }

    private fun click(tag: String) {
        compose.onNodeWithTag(tag).performScrollTo().performClick()
    }

    @Test fun routeRowsAreMergedButtons() {
        host(routeDraft(1))
        assertEquals(
            Role.Button,
            compose.onNodeWithTag("wizard.from").fetchSemanticsNode().config[SemanticsProperties.Role],
        )
        assertTrue(
            compose
                .onNodeWithTag(
                    "wizard.stop.1",
                ).fetchSemanticsNode()
                .config[SemanticsProperties.ContentDescription]
                .isNotEmpty(),
        )
        assertTrue(
            compose
                .onNodeWithTag(
                    "wizard.to",
                ).fetchSemanticsNode()
                .config[SemanticsProperties.ContentDescription]
                .isNotEmpty(),
        )
        assertTrue(
            compose
                .onNodeWithTag(
                    "wizard.remove-stop.1",
                ).fetchSemanticsNode()
                .config[SemanticsProperties.ContentDescription]
                .isNotEmpty(),
        )
    }

    @Test fun swapAndStopGroupAreLabelled() {
        host()
        assertTrue(
            compose
                .onNodeWithTag(
                    "wizard.swap",
                ).fetchSemanticsNode()
                .config[SemanticsProperties.ContentDescription]
                .isNotEmpty(),
        )
        assertTrue(
            compose
                .onNodeWithTag(
                    "wizard.route.stops",
                ).fetchSemanticsNode()
                .config[SemanticsProperties.ContentDescription]
                .isNotEmpty(),
        )
    }

    @Test fun titleIsAHeadingAndNoticeIsMerged() {
        host()
        assertTrue(SemanticsProperties.Heading in compose.onNodeWithTag("wizard.title").fetchSemanticsNode().config)
        assertTrue(
            compose
                .onNodeWithTag("wizard.audienceNotice")
                .fetchSemanticsNode()
                .config.isMergingSemanticsOfDescendants,
        )
    }
}
