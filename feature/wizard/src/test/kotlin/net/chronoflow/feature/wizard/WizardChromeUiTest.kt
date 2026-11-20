package net.chronoflow.feature.wizard

import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.wizard.model.MemberWizardSnapshot
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.feature.wizard.checking.WizardCheckingUi
import net.chronoflow.feature.wizard.finetune.WizardFineTuneUi
import net.chronoflow.feature.wizard.ui.WizardChromeUi
import net.chronoflow.feature.wizard.ui.WizardUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WizardChromeUiTest {
    @Test fun checkingChromeUsesTheSuppliedProjection() {
        val active = checkingActive(failure = DataFailure.Throttled(42.0, 429), retry = 42)
        val checking = WizardCheckingUi.from(active)!!.copy(retrySeconds = 7)
        val chrome = WizardChromeUi.from(active, checking = checking)
        assertEquals(checking.button, chrome.primaryLabel)
        assertFalse(chrome.continueEnabled)
        assertEquals("checking", chrome.footerId)
    }

    @Test fun fineTuneChromeUsesTheSuppliedProjection() {
        val active = fineTuneActive()
        val fineTune = WizardFineTuneUi.from(active, TimeFormat.Value24, null)!!.copy(reviewing = true)
        val chrome = WizardChromeUi.from(active, fineTune = fineTune)
        assertEquals(fineTune.button, chrome.primaryLabel)
        assertTrue(chrome.primaryBusy)
    }

    @Test fun stateAndChromeShareTheCheckingValues() {
        val state = WizardUiState.from(checkingActive(failure = DataFailure.Offline))
        assertEquals(state.checking!!.button, state.chrome.primaryLabel)
        assertFalse(state.chrome.primaryBusy)
        assertTrue(state.chrome.continueEnabled)
    }

    @Test fun stateAndChromeShareTheFineTuneValues() {
        val active = fineTuneActive(reviewing = true)
        val state = WizardUiState.from(active)
        assertEquals(state.fineTune!!.button, state.chrome.primaryLabel)
        assertEquals(state.fineTune.reviewing, state.chrome.primaryBusy)
        assertFalse(state.chrome.continueEnabled)
    }

    private fun fineTuneActive(reviewing: Boolean = false): TripWizardUiState.Active {
        val draft = routeDraft().let { it.copy(state = it.state.copy(step = MemberWizardStep.fineTune)) }
        return TripWizardUiState.Active(MemberWizardSnapshot(draft, reviewing = reviewing), .8, emptyList())
    }
}
