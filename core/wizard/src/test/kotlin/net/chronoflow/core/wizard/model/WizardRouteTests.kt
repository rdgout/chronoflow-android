package net.chronoflow.core.wizard.model

import net.chronoflow.core.wizard.generationInput
import net.chronoflow.core.wizard.wizardAirport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith

@RunWith(Enclosed::class)
class WizardRouteTests {
    class Routes {
        private val stop = wizardAirport("DXB", "Dubai", "Asia/Dubai")
        private val second = wizardAirport("HEL", "Helsinki", "Europe/Helsinki")

        @Test
        fun splitsLastLegAndJoinsOnlyTheRemovedStopsFlights() {
            var form = WizardForm.from(generationInput())
            val first = form.legs[0]
            val destination = first.toIata
            form =
                form
                    .insertStop(stop, 0)
                    .updateLeg(0) { it.copy(arriveLocal = "2026-11-11T08:00") }
                    .updateLeg(1) { it.copy(departLocal = "2026-11-11T10:00", arriveLocal = "2026-11-12T09:00") }
            val firstFlight = form.legs[0]
            form = form.insertStop(second, 1)
            assertEquals(listOf(first.fromIata, "DXB", "HEL", destination), form.route().map { it.iata })
            assertEquals(firstFlight, form.legs[0])
            assertEquals("2026-11-11T10:00", form.legs[1].departLocal)
            assertTrue(form.legs[1].arriveLocal.isEmpty())
            assertTrue(form.legs[2].departLocal.isEmpty())
            assertTrue(form.legs[2].arriveLocal.isEmpty())
            form = form.updateLeg(2) { it.copy(arriveLocal = "2026-11-12T12:00") }.removeStop(2)
            assertEquals(listOf(first.fromIata, "DXB", destination), form.route().map { it.iata })
            assertEquals(firstFlight, form.legs[0])
            assertEquals("2026-11-11T10:00", form.legs[1].departLocal)
            assertEquals("2026-11-12T12:00", form.legs[1].arriveLocal)
            form = form.makeDirect()
            assertEquals(listOf(first.fromIata, destination), form.route().map { it.iata })
            assertEquals(first.departLocal, form.legs[0].departLocal)
            assertEquals("2026-11-12T12:00", form.legs[0].arriveLocal)
            assertEquals(1, form.stays.size)
        }

        @Test
        fun returnStopsKeepBoundaryAndDirectDoesNotTouchTheOtherRoute() {
            var form =
                WizardForm
                    .from(generationInput())
                    .insertStop(stop, 0)
                    .addReturn()
                    .setRouteAirport(second, 1, home = true)
                    .insertStop(stop, 2)
            val home = form.legs.drop(2)
            form = form.makeDirect()
            assertEquals(1, form.returnBoundary)
            assertEquals(3, form.legs.size)
            assertEquals(home, form.legs.drop(1))
            val outbound = form.legs[0]
            assertEquals(form, form.removeStop(form.returnBoundary))
            form = form.makeDirect(home = true)
            assertEquals(2, form.legs.size)
            assertEquals(outbound, form.legs[0])
            assertEquals(listOf(outbound.toIata, "HEL"), form.route(home = true).map { it.iata })
        }

        @Test
        fun editingAStopDerivesBothAdjacentEndpointsAndHonorsTheTotalLimit() {
            var form = WizardForm.from(generationInput()).insertStop(stop, 0).setRouteAirport(second, 1)
            assertEquals("HEL", form.legs[0].toIata)
            assertEquals("HEL", form.legs[1].fromIata)
            form = form.addReturn()
            form =
                (form.legs.size until WizardForm.maxFlights).fold(form) { value, _ ->
                    value.insertStop(
                        stop,
                        value.returnBoundary - 1,
                    )
                }
            val limit = form
            form = form.insertStop(second, form.returnBoundary - 1).insertStop(second, form.legs.size - 1)
            assertEquals(limit, form)
            assertEquals(16, form.stays.size)
        }
    }
}
