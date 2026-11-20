package net.chronoflow.feature.wizard

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardScreen
import net.chronoflow.feature.wizard.ui.WizardUiState
import org.junit.Assert.assertEquals
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
class TripWizardActionsScreenTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain =
        RuleChain
            .outerRule(
                ProfileRule(DeviceProfiles.pixel8, language = net.chronoflow.core.model.config.AppLanguage.Nl),
            ).around(compose)
    private val events = mutableListOf<WizardEvent>()

    private fun show(
        board: String = "Edit",
        dialog: Boolean = false,
    ) {
        val state =
            WizardUiState.from(
                editReadyActive(board).copy(
                    firstScreen = true,
                    path = listOf(net.chronoflow.core.wizard.model.MemberWizardDestination.ready),
                ),
                dialog = dialog,
            )
        compose.setContent {
            WizardTestHost {
                WizardScreen(state) {
                    if (it != WizardEvent.ReadyTick && it != WizardEvent.ReadyForeground) events.add(it)
                }
            }
        }
    }

    @Test fun testUntouchedEditHasDisabledUpdateButton() {
        show()
        compose.onNodeWithTag("wizard.ready.continue").assertIsNotEnabled()
        compose.onNodeWithTag("wizard.title").assertExists()
        compose.onNodeWithTag("wizard.back").assertDoesNotExist()
    }

    @Test fun testEditNoCreditsHasCloseWithoutBilling() {
        show("EditReturnNoCredits")
        compose.onNodeWithTag("wizard.ready.no-credits").assertHasClickAction()
        compose.onNodeWithTag("wizard.ready.continue").assertDoesNotExist()
        compose.onNodeWithTag("wizard.ready.footer").assertExists()
    }

    @Test fun testEditDiscardUsesNativeAlert() {
        show("EditReturn", dialog = true)
        compose.onNodeWithTag("cf.dialog.dismiss").assertExists()
        compose.onNodeWithTag("cf.dialog.confirm").performClick()
        assertEquals(listOf(WizardEvent.ConfirmDiscard), events)
    }

    @Test fun closeAsksTheViewModelToApplyTheEditRules() {
        show()
        compose.onNodeWithTag("wizard.close").performClick()
        assertEquals(listOf(WizardEvent.RequestClose), events)
        compose.onNodeWithTag("cf.dialog.confirm").assertDoesNotExist()
    }

    @Test fun keepEditingDismissesTheAlert() {
        show("EditReturn", dialog = true)
        compose.onNodeWithTag("cf.dialog.dismiss").performClick()
        assertEquals(listOf(WizardEvent.DismissDiscard), events)
    }
}
