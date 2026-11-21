package net.chronoflow.feature.wizard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.WizardMode
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardScreen
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
class GuestDiscardDialogTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)

    @Test fun keepEditingReturnsToOutbound() {
        val draft =
            routeDraft().let {
                it.copy(state = it.state.copy(mode = WizardMode.guest, step = MemberWizardStep.outbound))
            }
        var state by mutableStateOf(screenState(draft).copy(discardDialogVisible = true))
        compose.setContent {
            WizardTestHost {
                WizardScreen(state) { event ->
                    if (event == WizardEvent.DismissDiscard) state = state.copy(discardDialogVisible = false)
                }
            }
        }
        compose.onNodeWithTag("cf.dialog.dismiss").performClick()
        compose.onNodeWithTag("cf.dialog.confirm").assertDoesNotExist()
        compose.onNodeWithTag("wizard.outbound.continue").assertExists()
    }
}
