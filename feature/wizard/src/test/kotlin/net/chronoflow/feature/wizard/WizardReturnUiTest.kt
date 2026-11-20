package net.chronoflow.feature.wizard

import net.chronoflow.core.wizard.model.MemberWizardSnapshot
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.feature.wizard.returnchoice.WizardReturnUi
import net.chronoflow.feature.wizard.ui.WizardChromeUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WizardReturnUiTest {
    @Test fun oneWayIsSelectedByDefault() {
        val ui = WizardReturnUi.from(flightDraft(), false)
        assertTrue(ui.oneWay.selected)
        assertFalse(ui.addReturn.selected)
        assertTrue(ui.oneWay.enabled)
        assertTrue(ui.addReturn.enabled)
    }

    @Test fun pendingReturnIsSelectedAndBothChoicesAreDisabled() {
        val draft = flightDraft(home = true)
        val ui = WizardReturnUi.from(draft, true)
        assertFalse(ui.oneWay.selected)
        assertTrue(ui.addReturn.selected)
        assertFalse(ui.oneWay.enabled)
        assertFalse(ui.addReturn.enabled)
        assertFalse(WizardReturnUi.from(draft, false).addReturn.selected)
    }

    @Test fun pendingOneWayKeepsItsSelectionWhileDisabled() {
        val ui = WizardReturnUi.from(flightDraft(), true)
        assertTrue(ui.oneWay.selected)
        assertFalse(ui.addReturn.selected)
        assertFalse(ui.oneWay.enabled)
        assertFalse(ui.addReturn.enabled)
    }

    @Test fun sixteenFlightsDisableAddingReturn() {
        val draft = routeDraft(stops = 15)
        val ui = WizardReturnUi.from(draft, false)
        assertTrue(ui.oneWay.enabled)
        assertFalse(ui.addReturn.enabled)
        assertFalse(ui.addReturn.selected)
    }

    @Test fun chromeShowsOnlyTheAnswerHint() {
        val draft = flightDraft().let { it.copy(state = it.state.copy(step = MemberWizardStep.returnChoice)) }
        val chrome = WizardChromeUi.from(TripWizardUiState.Active(MemberWizardSnapshot(draft), .45, emptyList()))
        assertNull(chrome.footerId)
        assertNotNull(chrome.footerHint)
        assertEquals("Tap an answer to continue.", chrome.footerHint!!.key)
    }
}
