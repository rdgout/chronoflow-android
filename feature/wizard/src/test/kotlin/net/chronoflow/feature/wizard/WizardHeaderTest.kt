@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.feature.wizard

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.wizard.model.MemberWizardStep
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
class WizardHeaderTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)

    @Test fun theForwardSlideKeepsTheOutgoingWhereContent() {
        val draft = routeDraft()
        val state = mutableStateOf(screenState(draft))
        compose.mainClock.autoAdvance = false
        compose.setContent {
            net.chronoflow.core.testing.compose.ProfileHost(DeviceProfiles.pixel8) {
                net.chronoflow.core.designsystem.theme.ChronoFlowTheme(reduceMotion = false) {
                    WizardScreen(state.value, {})
                }
            }
        }
        compose.runOnIdle {
            state.value =
                screenState(draft.copy(state = draft.state.copy(step = MemberWizardStep.outbound)))
        }
        compose.mainClock.advanceTimeBy(150)
        compose.onNodeWithTag("wizard.route").assertExists()
        compose.onNodeWithTag("wizard.seam.outbound").assertExists()
        compose.onNodeWithTag("wizard.seam.whereTo").assertDoesNotExist()
    }

    @Test fun whereHasCloseAndNoBack() {
        compose.setContent { WizardTestHost { WizardScreen(screenState(), {}) } }
        compose.onNodeWithTag("wizard.back").assertDoesNotExist()
        assertTrue(
            compose
                .onNodeWithTag(
                    "wizard.close",
                ).fetchSemanticsNode()
                .config[SemanticsProperties.ContentDescription]
                .isNotEmpty(),
        )
        assertEquals(
            0.15f,
            compose
                .onNodeWithTag(
                    "wizard.progress",
                ).fetchSemanticsNode()
                .config[SemanticsProperties.ProgressBarRangeInfo]
                .current,
        )
    }

    @Test fun homeRouteHasBack() {
        compose.setContent { WizardTestHost { WizardScreen(screenState(routeDraft(home = true)), {}) } }
        compose.onNodeWithTag("wizard.back").assertExists()
    }

    @Test fun seamHasNoFooterAndTitleFocusReturnsOnStepChange() {
        val where = screenState()
        val draft = routeDraft()
        val state = mutableStateOf(where)
        compose.setContent { WizardTestHost { WizardScreen(state.value, {}) } }
        compose.runOnIdle {
            state.value =
                screenState(draft.copy(state = draft.state.copy(step = MemberWizardStep.outbound)))
        }
        compose.onNodeWithTag("wizard.seam.outbound").assertExists()
        compose.onNodeWithTag("wizard.where.continue").assertDoesNotExist()
        compose.runOnIdle { state.value = where }
        compose.onNodeWithTag("wizard.title").assertIsFocused()
    }
}
