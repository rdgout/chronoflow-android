package net.chronoflow.feature.wizard

import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.wizard.generation.TripEditContext
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.WizardEnd
import net.chronoflow.feature.wizard.returnchoice.WizardReturnUi
import net.chronoflow.feature.wizard.ui.WizardCopy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

internal fun webEndDraft(kind: String = "nights"): MemberWizardDraft {
    val base = flightDraft()
    val form = base.form.copy(end = WizardEnd(kind = kind, nights = 9, checkOutDate = "2026-10-21"))
    return base.copy(form = form, state = base.state.copy(edit = TripEditContext("t1", "source"), originalForm = form))
}

class ReturnChoiceUiTest {
    @Test fun nightsShowTheSelectedWebChoice() {
        val ui = WizardReturnUi.from(webEndDraft(), false)
        val web = checkNotNull(ui.web)
        assertEquals("nights", web.kind)
        assertEquals(WizardCopy("Ends after {{count}} nights", mapOf("count" to "9")), web.title)
        assertEquals(WizardCopy("Set on the website. Keep it, or add your flight home."), web.hint)
        assertTrue(web.selected)
        assertFalse(ui.oneWay.selected)
    }

    @Test fun checkoutUsesTheReturnSummary() {
        val ui = WizardReturnUi.from(webEndDraft("check_out"), false)
        assertEquals(WizardCopy("Check-out · {{date}}", mapOf("date" to "Wed 21 Oct")), ui.web!!.title)
    }

    @Test fun checkoutUsesTheWizardLanguage() {
        val ui = WizardReturnUi.from(webEndDraft("check_out"), false, language = Language.Nl)
        assertEquals("wo 21 okt", ui.web!!.title.arguments["date"])
    }

    @Test fun oneWayHasNoWebChoice() {
        assertNull(WizardReturnUi.from(flightDraft(), false).web)
    }

    @Test fun addingAReturnToAnEditShowsTheCreditHint() {
        assertEquals(
            WizardCopy("We plan the shift back home too · 1 credit"),
            WizardReturnUi.from(webEndDraft(), false).addReturn.hint,
        )
    }

    @Test fun existingReturnKeepsThePlainHint() {
        val base = webEndDraft()
        val draft = base.copy(state = base.state.copy(originalForm = base.form.addReturn()))
        assertEquals(WizardCopy("We plan the shift back home too"), WizardReturnUi.from(draft, false).addReturn.hint)
    }

    @Test fun createKeepsThePlainHint() {
        assertEquals(
            WizardCopy("We plan the shift back home too"),
            WizardReturnUi.from(flightDraft(), false).addReturn.hint,
        )
    }
}
