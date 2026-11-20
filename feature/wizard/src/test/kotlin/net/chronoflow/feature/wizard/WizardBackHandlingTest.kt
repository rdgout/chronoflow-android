package net.chronoflow.feature.wizard

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.wizard.model.WizardAirportSheet
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardScreen
import net.chronoflow.feature.wizard.ui.AirportSheetUi
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
class WizardBackHandlingTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)
    private val events = mutableListOf<WizardEvent>()
    private lateinit var dispatcher: OnBackPressedDispatcher

    private fun host(state: WizardUiState = screenState()) {
        compose.setContent {
            dispatcher = checkNotNull(LocalOnBackPressedDispatcherOwner.current).onBackPressedDispatcher
            WizardTestHost { WizardScreen(state) { if (it !is WizardEvent.Query) events.add(it) } }
        }
    }

    @Test fun backOnWhereRequestsClose() {
        host()
        compose.runOnIdle { dispatcher.onBackPressed() }
        assertEquals(listOf(WizardEvent.RequestClose), events)
    }

    @Test fun backWithTheDialogOpenDismissesIt() {
        host(screenState().copy(discardDialogVisible = true))
        compose.runOnIdle { dispatcher.onBackPressed() }
        assertEquals(listOf(WizardEvent.DismissDiscard), events)
    }

    @Test fun backWithTheSheetOpenCancelsTheSheet() {
        host(screenState().copy(sheet = AirportSheetUi(WizardAirportSheet(0, WizardAirportSheet.Side.from), null)))
        compose.runOnIdle { dispatcher.onBackPressed() }
        assertEquals(listOf(WizardEvent.CancelAirport), events)
    }

    @Test fun backOnTheHomeRoutePops() {
        host(screenState(routeDraft(home = true)))
        compose.runOnIdle { dispatcher.onBackPressed() }
        assertEquals(listOf(WizardEvent.Back), events)
    }

    @Test fun discardDialogHasBothActions() {
        host(screenState().copy(discardDialogVisible = true))
        compose.onNodeWithTag("cf.dialog.dismiss").assertExists()
        compose.onNodeWithTag("cf.dialog.confirm").performClick()
        assertEquals(listOf(WizardEvent.ConfirmDiscard), events)
    }
}
