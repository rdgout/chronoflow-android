package net.chronoflow.core.plan.step

import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.plan.PageCopy
import net.chronoflow.core.plan.actions.TripAction
import net.chronoflow.core.plan.actions.TripActionsAvailability
import net.chronoflow.core.plan.actions.tripActionsHeader
import net.chronoflow.core.plan.testPageCopy
import net.chronoflow.core.plan.tripDatesText
import net.chronoflow.core.testing.MockTripTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class TripActionsTests(
    private val bits: Int,
) {
    @Test fun flagsConnectionAndBusyGateBothActionsIndependently() {
        val edit = bits and 1 != 0
        val duplicate = bits and 2 != 0
        val online = bits and 4 != 0
        val busy = bits and 8 != 0
        val value = TripActionsAvailability(edit, duplicate, online, busy)
        assertEquals(edit && online && !busy, value.enabled(TripAction.Edit))
        assertEquals(duplicate && online && !busy, value.enabled(TripAction.Duplicate))
    }

    @Test fun headerIncludesDatesConnectionsAndReturnWithPairedCopy() {
        val template = MockTripTemplates.templates().first()
        val card = template.card.copy(connections = listOf(template.card.to))
        val detail =
            template.detail.copy(
                trip =
                    template.detail.trip.copy(
                        legs =
                            template.detail.trip.legs.map {
                                it.copy(returnLeg = true)
                            },
                    ),
            )
        val language = if (bits and 1 == 0) Language.En else Language.Nl
        val copy = PageCopy(language, testPageCopy)
        val header = tripActionsHeader(card, detail, copy)
        assertEquals("${card.from.city ?: card.from.iata} → ${card.to.city ?: card.to.iata}", header.title)
        assertTrue(header.subtitle.contains(" · via ${card.to.city ?: card.to.iata} · "))
        assertTrue(header.subtitle.endsWith(if (language == Language.En) "with return" else "met terugreis"))
        val noPlan = tripActionsHeader(card, null, copy)
        assertTrue(noPlan.subtitle.startsWith(tripDatesText(card.departLocal, null, language, pageCopy = copy)))
        assertFalse(noPlan.subtitle.contains(copy("with return")))
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "flags-{0}")
        fun cases() = (0..15).map { arrayOf(it) }
    }
}
