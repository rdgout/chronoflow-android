@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.feature.wizard

import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.ui.l10n.generated.CopyIndex
import net.chronoflow.core.ui.l10n.generated.CopyTable
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.WizardConnection
import net.chronoflow.core.wizard.model.WizardPendingDateTime
import net.chronoflow.feature.wizard.debug.WizardBoardCaptureSeed
import net.chronoflow.feature.wizard.times.WizardPicker
import net.chronoflow.feature.wizard.times.WizardPickerKind
import net.chronoflow.feature.wizard.times.WizardTimesUi
import net.chronoflow.feature.wizard.ui.wizardCopy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.time.LocalDate
import java.util.Locale

@RunWith(Parameterized::class)
class WizardTimesUiTest(
    private val board: String,
) {
    @Test fun derivesTitlesCompletenessErrorsFactsAndPickerInputs() {
        val draft = WizardBoardCaptureSeed.draft(board, Locale.ENGLISH)
        val home = draft.state.step == MemberWizardStep.returnFlight
        val ui = WizardTimesUi.from(draft, home, TimeFormat.Value12, WizardBoardCaptureSeed.now)
        val base = WizardBoardCaptureSeed.baseName(board)
        assertEquals(TimeFormat.Value12, ui.timeFormat)
        assertEquals(
            when {
                home -> "When do you fly home?"
                ui.cards.size > 1 -> "When are your flights?"
                else -> "When is your flight?"
            },
            ui.title.key,
        )
        assertNotNull(CopyIndex.find(CopyTable.Wizard, ui.title.key))
        assertNotNull(CopyIndex.find(CopyTable.Wizard, ui.subtitle.key))
        assertEquals(
            base !in
                setOf("WizardOutboundEmpty", "WizardOutboundStopEmpty", "WizardOutboundThree", "WizardReturnFlight"),
            ui.complete,
        )
        assertEquals(base.endsWith("Past"), ui.banner != null)
        assertEquals(
            when (base) {
                "WizardOutbound", "WizardOutboundPast" -> 695
                "WizardReturnFlightPast" -> 725
                else -> null
            },
            ui.flightMinutes,
        )
        assertEquals(
            draft.fieldErrors.keys
                .filter { it.startsWith("legs.") }
                .toSet(),
            ui.cards
                .flatMap { card ->
                    listOfNotNull(
                        card.departure.error?.let {
                            "legs.${card.index}.departLocal"
                        },
                        card.arrival.error?.let { "legs.${card.index}.arriveLocal" },
                    )
                }.toSet(),
        )
        val first = ui.cards.first()
        if (!home) assertNull(first.departure.minimumDate)
        if (base == "WizardOutboundStop") {
            assertEquals(LocalDate.of(2026, 10, 12), first.arrival.minimumDate)
            assertEquals(WizardConnection.Connection("Dubai", 160), ui.cards[1].connection)
        }
        if (base ==
            "WizardOutboundOvernight"
        ) {
            assertEquals(WizardConnection.Overnight("Dubai", 1), ui.cards[1].connection)
        }
        if (base == "WizardOutboundStopEmpty") assertEquals(WizardConnection.Stop("Dubai"), ui.cards[1].connection)
        if (base == "WizardOutboundThree") assertEquals(WizardConnection.Stop("Bangkok"), ui.cards[2].connection)
        ui.banner?.let { assertNotNull(CopyIndex.find(CopyTable.Wizard, it.key)) }
        ui.cards.forEach { card ->
            assertNotNull(CopyIndex.find(CopyTable.Wizard, card.departure.zone.key))
            assertNotNull(CopyIndex.find(CopyTable.Wizard, card.arrival.zone.key))
            card.departure.error?.let { assertNotNull(CopyIndex.find(CopyTable.Wizard, it.wizardCopy().key)) }
            card.arrival.error?.let { assertNotNull(CopyIndex.find(CopyTable.Wizard, it.wizardCopy().key)) }
        }
    }

    @Test fun halfPairDoesNotEnableContinue() {
        val draft = WizardBoardCaptureSeed.draft("WizardOutboundEmpty", Locale.ENGLISH)
        val half =
            draft.copy(
                state =
                    draft.state.copy(
                        pendingDateTimes =
                            mapOf("${draft.form.legs[0].id}.departLocal" to WizardPendingDateTime("2026-10-12")),
                    ),
            )
        val ui =
            WizardTimesUi.from(
                half,
                false,
                now = WizardBoardCaptureSeed.now,
                picker = WizardPicker(0, false, WizardPickerKind.Date),
            )
        assertFalse(ui.complete)
        assertEquals(LocalDate.of(2026, 10, 12), ui.picker!!.initialDate)
        assertNull(ui.cards[0].departure.timePart)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases() =
            WizardFlightBoardSeedTest.expected.keys.filter {
                it.startsWith("WizardOutbound") ||
                    it.startsWith("WizardReturnFlight")
            }
    }
}
