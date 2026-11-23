package net.chronoflow.feature.wizard

import net.chronoflow.feature.wizard.ui.WizardUiState
import org.junit.Assert.assertEquals
import org.junit.Test

class WizardDiscardUiTest {
    @Test fun editUsesTheChangesTitleActionAndMessage() {
        val ui = WizardUiState.from(checkingActive(editing = true), dialog = true)
        assertEquals("Discard your changes?", ui.discardTitle.key)
        assertEquals("Discard changes", ui.discardConfirmLabel.key)
        assertEquals("Your trip stays as it was.", ui.discardMessage.key)
    }

    @Test fun newMemberTripUsesTheTripTitleActionAndMessage() {
        val ui = WizardUiState.from(checkingActive(), dialog = true)
        assertEquals("Discard this trip?", ui.discardTitle.key)
        assertEquals("Discard", ui.discardConfirmLabel.key)
        assertEquals("What you entered is not saved.", ui.discardMessage.key)
    }

    @Test fun newGuestTripKeepsTheWelcomeMessage() {
        val ui = WizardUiState.from(checkingActive(guest = true), dialog = true)
        assertEquals("Discard this trip?", ui.discardTitle.key)
        assertEquals("Discard", ui.discardConfirmLabel.key)
        assertEquals("What you entered is not saved. You go back to the welcome screen.", ui.discardMessage.key)
    }
}
