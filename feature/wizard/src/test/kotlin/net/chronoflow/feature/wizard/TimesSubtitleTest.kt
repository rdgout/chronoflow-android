package net.chronoflow.feature.wizard

import net.chronoflow.core.wizard.generation.TripEditContext
import net.chronoflow.core.wizard.model.clearDuplicateDates
import net.chronoflow.feature.wizard.times.WizardTimesUi
import net.chronoflow.feature.wizard.ui.WizardCopy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.Instant

internal fun duplicateDraft() =
    flightDraft(home = true).let { draft ->
        draft.copy(
            form = draft.form.clearDuplicateDates(),
            state =
                draft.state.copy(
                    copiedRoute = "Amsterdam → Tokio",
                    step = net.chronoflow.core.wizard.model.MemberWizardStep.outbound,
                    review = null,
                    fromLastTrip = false,
                ),
        )
    }

class TimesSubtitleTest {
    @Test fun copiedOutboundShowsTheRouteAndEmptyDates() {
        val ui = WizardTimesUi.from(duplicateDraft(), false, now = Instant.EPOCH)
        assertEquals(
            WizardCopy(
                "Copied from {{route}}. Choose the new dates.",
                mapOf("route" to "Amsterdam → Tokio"),
            ),
            ui.subtitle,
        )
        assertEquals(
            null,
            ui.cards
                .single()
                .departure.datePart,
        )
        assertEquals(
            null,
            ui.cards
                .single()
                .departure.timePart,
        )
        assertFalse(ui.complete)
    }

    @Test fun homeKeepsItsTitleAndSubtitle() {
        val ui = WizardTimesUi.from(duplicateDraft(), true, now = Instant.EPOCH)
        assertEquals(WizardCopy("When do you fly home?"), ui.title)
        assertEquals(WizardCopy("Times are local at each airport."), ui.subtitle)
    }

    @Test fun editKeepsTheDefaultSubtitleEvenWithACopiedRoute() {
        val draft = duplicateDraft().let { it.copy(state = it.state.copy(edit = TripEditContext("t1", "source"))) }
        assertEquals(
            WizardCopy("Times are local at each airport."),
            WizardTimesUi.from(draft, false, now = Instant.EPOCH).subtitle,
        )
    }
}
