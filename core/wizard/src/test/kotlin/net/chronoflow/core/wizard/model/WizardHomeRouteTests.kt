@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.core.wizard.model

import kotlinx.coroutines.test.runTest
import net.chronoflow.core.testing.wizard.generationInput
import net.chronoflow.core.testing.wizard.wizardAirport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class WizardHomeRouteTests {
    @Test fun homeStopChoicesUseTheReturnBoundaryAndDirectPreservesOutbound() =
        runTest(timeout = 60.seconds) {
            val fixture = WizardModelFixture(backgroundScope, testScheduler)
            val model = fixture.model
            model.form =
                WizardForm
                    .from(generationInput())
                    .insertStop(wizardAirport("LHR", "London", "Europe/London"), 0)
                    .addReturn()
                    .setRouteAirport(wizardAirport("AMS", "Amsterdam", "Europe/Amsterdam"), 1, home = true)
                    .updateLeg(2) { it.copy(departLocal = "2026-11-20T09:15", arriveLocal = "2026-11-20T19:45") }
            model.state = model.state.copy(step = MemberWizardStep.returnRoute)
            val outbound = model.form.legs.take(2)
            val stays = model.form.stays.take(2)
            val origin = model.form.legs[2].fromIata
            model.chooseAirport(
                wizardAirport("DXB", "Dubai", "Asia/Dubai"),
                WizardAirportSheet(2, WizardAirportSheet.Side.stop),
            )
            assertEquals(listOf(origin, "DXB", "AMS"), model.form.route(true).map { it.iata })
            assertEquals("2026-11-20T09:15", model.form.legs[2].departLocal)
            assertTrue(
                model.form.legs[2]
                    .arriveLocal
                    .isEmpty(),
            )
            assertTrue(
                model.form.legs[3]
                    .departLocal
                    .isEmpty() &&
                    model.form.legs[3]
                        .arriveLocal
                        .isEmpty(),
            )
            model.chooseAirport(
                wizardAirport("HEL", "Helsinki", "Europe/Helsinki"),
                WizardAirportSheet(3, WizardAirportSheet.Side.stop),
            )
            assertEquals(listOf(origin, "DXB", "HEL", "AMS"), model.form.route(true).map { it.iata })
            model.chooseAirport(
                wizardAirport("CDG", "Paris", "Europe/Paris"),
                WizardAirportSheet(3, WizardAirportSheet.Side.stop, true),
            )
            assertEquals(5, model.form.legs.size)
            assertEquals(2, model.form.returnBoundary)
            assertEquals(listOf(origin, "DXB", "CDG", "AMS"), model.form.route(true).map { it.iata })
            assertEquals("Paris", model.form.legs[3].toCity)
            assertEquals("Paris", model.form.legs[4].fromCity)
            assertEquals(outbound, model.form.legs.take(2))
            assertEquals(stays, model.form.stays.take(2))
            model.setDateTime("2026-11-20T19:45", 4, true)
            model.makeDirect(true)
            assertEquals(listOf(origin, "AMS"), model.form.route(true).map { it.iata })
            assertEquals(2, model.form.returnBoundary)
            assertEquals(3, model.form.legs.size)
            assertEquals("2026-11-20T09:15", model.form.legs[2].departLocal)
            assertEquals("2026-11-20T19:45", model.form.legs[2].arriveLocal)
            assertEquals(outbound, model.form.legs.take(2))
            assertEquals(stays, model.form.stays.take(2))
            assertEquals(model.form.legs.size, model.form.stays.size)
            model.flush()
            assertEquals(model.draft, fixture.store.last)
            fixture.close()
        }
}
