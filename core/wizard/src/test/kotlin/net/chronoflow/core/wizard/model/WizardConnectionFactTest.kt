package net.chronoflow.core.wizard.model

import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.contracts.ApiReviewItinerary
import net.chronoflow.core.model.contracts.ApiReviewLayover
import net.chronoflow.core.model.contracts.SegmentKindCode
import net.chronoflow.core.model.contracts.TripDirection
import net.chronoflow.core.testing.wizard.wizardAirport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WizardConnectionFactTest {
    private val airports = mapOf("DXB" to wizardAirport("DXB", "Dubai", "Asia/Dubai"))

    private fun stop(
        arrival: String = "2026-10-12T23:25",
        departure: String = "2026-10-13T02:05",
    ) = timingRoute("AMS", "DXB", "NRT")
        .updateLeg(0) { it.copy(toCity = "Dubai", arriveLocal = arrival) }
        .updateLeg(1) { it.copy(departLocal = departure) }

    @Test fun stopBoardHasAConnectionOf160Minutes() {
        assertEquals(WizardConnection.Connection("Dubai", 160), WizardOutboundRules.connectionFact(stop(), 1, airports))
        assertEquals(
            FieldMessage(
                "Connection in {{airport}} · {{duration}}",
                mapOf(
                    "airport" to JsonPrimitive("Dubai"),
                    "duration" to JsonPrimitive("160"),
                ),
            ),
            WizardOutboundRules.connection(stop(), 1, airports) { it.toString() },
        )
    }

    @Test fun overnightSeedHasOneCompleteNight() {
        val form = stop("2026-10-12T22:25", "2026-10-14T02:05")
        assertEquals(WizardConnection.Overnight("Dubai", 1), WizardOutboundRules.connectionFact(form, 1, airports))
        assertEquals(
            FieldMessage(
                "Stop in {{airport}} · {{count}} complete night",
                mapOf("airport" to JsonPrimitive("Dubai"), "count" to JsonPrimitive(1)),
            ),
            WizardOutboundRules.connection(form, 1, airports) { it.toString() },
        )
    }

    @Test fun emptyStopHasOnlyTheAirport() {
        val form = stop("", "")
        assertEquals(WizardConnection.Stop("Dubai"), WizardOutboundRules.connectionFact(form, 1, airports))
        assertEquals(
            FieldMessage(
                "Stop in {{airport}}",
                mapOf("airport" to JsonPrimitive("Dubai")),
            ),
            WizardOutboundRules.connection(form, 1, airports) {
                it.toString()
            },
        )
    }

    @Test fun threeFlightsSecondChipIsAStopInBangkok() {
        val form =
            stop().insertStop(wizardAirport("BKK", "Bangkok", "Asia/Bangkok"), 1).updateLeg(1) {
                it.copy(arriveLocal = "2026-10-13T11:20")
            }
        assertEquals(WizardConnection.Stop("Bangkok"), WizardOutboundRules.connectionFact(form, 2, airports))
    }

    @Test fun itineraryFactWinsOverLocalTimes() {
        val form =
            stop().copy(
                itinerary =
                    ApiReviewItinerary(
                        emptyList(),
                        listOf(ApiReviewLayover(TripDirection.Outbound, 0, SegmentKindCode.Connection, 95, 0)),
                    ),
            )
        assertEquals(WizardConnection.Connection("Dubai", 95), WizardOutboundRules.connectionFact(form, 1, airports))
    }

    @Test fun firstFlightAndReturnBoundaryHaveNoChip() {
        assertNull(WizardOutboundRules.connectionFact(stop(), 0, airports))
        assertNull(
            WizardOutboundRules.connectionFact(
                stop().copy(end = WizardEnd(kind = "return_legs", returnFromLeg = 1)),
                1,
                airports,
            ),
        )
        assertNull(WizardOutboundRules.connectionFact(stop(), 99, airports))
    }

    @Test fun absentCityUsesTheAirportCode() {
        assertEquals(
            WizardConnection.Connection("DXB", 160),
            WizardOutboundRules.connectionFact(
                stop().updateLeg(0) {
                    it.copy(toCity = null)
                },
                1,
                airports,
            ),
        )
    }
}
