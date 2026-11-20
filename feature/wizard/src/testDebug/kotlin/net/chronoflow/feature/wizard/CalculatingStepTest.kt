package net.chronoflow.feature.wizard

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.designsystem.theme.LocalReduceMotion
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.wizard.model.MemberWizardDestination
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardScreen
import net.chronoflow.feature.wizard.ui.WizardUiState
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
class CalculatingStepTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)
    private val events = mutableListOf<WizardEvent>()
    private lateinit var dispatcher: OnBackPressedDispatcher

    private fun show(reduceMotion: Boolean = true) {
        val base = readyActive()
        val draft = base.wizard.draft
        val state =
            WizardUiState.from(
                base.copy(
                    wizard =
                        base.wizard.copy(
                            draft = draft.copy(state = draft.state.copy(step = MemberWizardStep.calculating)),
                        ),
                    path = listOf(MemberWizardDestination.calculating),
                ),
            )
        compose.mainClock.autoAdvance = false
        compose.setContent {
            WizardTestHost {
                CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
                    dispatcher = checkNotNull(LocalOnBackPressedDispatcherOwner.current).onBackPressedDispatcher
                    WizardScreen(state, events::add)
                }
            }
        }
    }

    @Test fun calculatingHasNoCloseBackOrFooterAndAnnouncesTheTitle() {
        show()
        compose.onNodeWithTag("wizard.close").assertDoesNotExist()
        compose.onNodeWithTag("wizard.back").assertDoesNotExist()
        compose.onNodeWithTag("wizard.calculating.footer").assertDoesNotExist()
        compose.onNodeWithTag("wizard.calculating.card").assertExists()
        compose
            .onNodeWithTag(
                "wizard.title",
            ).assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test fun systemBackIsConsumedTwice() {
        show()
        compose.runOnIdle { dispatcher.onBackPressed() }
        compose.runOnIdle { dispatcher.onBackPressed() }
        compose.onNodeWithTag("wizard.calculating").assertExists()
        assertTrue(events.isEmpty())
    }

    @Test fun reducedMotionUsesTheClock() {
        show()
        compose.onNodeWithTag("wizard.calculating.static", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("wizard.calculating.spinner", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test fun motionUsesTheSpinner() {
        show(reduceMotion = false)
        compose.onNodeWithTag("wizard.calculating.static", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("wizard.calculating.spinner", useUnmergedTree = true).assertExists()
    }
}
