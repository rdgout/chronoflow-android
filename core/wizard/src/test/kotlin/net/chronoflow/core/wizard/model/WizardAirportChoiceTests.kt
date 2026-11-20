package net.chronoflow.core.wizard.model

import kotlinx.coroutines.test.runTest
import net.chronoflow.core.testing.wizard.generationInput
import net.chronoflow.core.testing.wizard.wizardAirport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

@RunWith(Enclosed::class)
class WizardAirportChoiceTests {
    @RunWith(Parameterized::class)
    class Neighbors(
        private val home: Boolean,
    ) {
        @Test fun searchAndSelectionRejectAdjacentDuplicatesInBothRoutes() =
            runTest(timeout = 60.seconds) {
                val fixture = WizardModelFixture(backgroundScope, testScheduler)
                val model = fixture.model
                if (home) {
                    model.form =
                        model.form
                            .insertStop(wizardAirport("LHR", "London", "Europe/London"), 0)
                            .addReturn()
                            .setRouteAirport(wizardAirport("AMS", "Amsterdam", "Europe/Amsterdam"), 1, true)
                }
                val draft = model.draft
                val start = if (home) model.form.returnBoundary else 0
                val origin =
                    model.form
                        .route(home)
                        .first()
                        .iata
                val destination =
                    model.form
                        .route(home)
                        .last()
                        .iata
                val add = WizardAirportSheet(start, WizardAirportSheet.Side.stop)
                assertFalse(model.airportIsAllowed(origin, add))
                assertFalse(model.airportIsAllowed(destination, add))
                model.chooseAirport(wizardAirport(origin, origin, "UTC"), add)
                model.chooseAirport(wizardAirport(destination, destination, "UTC"), add)
                assertEquals(draft, model.draft)
                val dubai = wizardAirport("DXB", "Dubai", "Asia/Dubai")
                val helsinki = wizardAirport("HEL", "Helsinki", "Europe/Helsinki")
                model.chooseAirport(dubai, add)
                val next = WizardAirportSheet(start + 1, WizardAirportSheet.Side.stop)
                assertFalse(model.airportIsAllowed("DXB", next))
                assertFalse(model.airportIsAllowed(destination, next))
                assertTrue(model.airportIsAllowed(origin, next))
                model.chooseAirport(helsinki, next)
                val replace = WizardAirportSheet(start, WizardAirportSheet.Side.stop, true)
                val before = model.draft
                assertFalse(model.airportIsAllowed(origin, replace))
                assertFalse(model.airportIsAllowed("HEL", replace))
                model.chooseAirport(wizardAirport(origin, origin, "UTC"), replace)
                model.chooseAirport(helsinki, replace)
                assertEquals(before, model.draft)
                assertTrue(model.airportIsAllowed("DXB", replace))
                assertTrue(model.airportIsAllowed(destination, replace))
                val from = WizardAirportSheet(start, WizardAirportSheet.Side.from)
                val to =
                    WizardAirportSheet(
                        start + 2,
                        if (home) WizardAirportSheet.Side.home else WizardAirportSheet.Side.to,
                    )
                assertFalse(model.airportIsAllowed("DXB", from))
                assertFalse(model.airportIsAllowed("HEL", to))
                model.chooseAirport(dubai, from)
                model.chooseAirport(helsinki, to)
                assertEquals(before, model.draft)
                assertTrue(model.form.routeComplete(home))
                fixture.close()
            }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "home={0}")
            fun cases() = listOf(false, true)
        }
    }

    class Direct {
        @Test fun directRouteStillAllowsTheDesignedSameAirportError() =
            runTest(timeout = 60.seconds) {
                val fixture = WizardModelFixture(backgroundScope, testScheduler)
                val model = fixture.model
                model.form = WizardForm.from(generationInput())
                val sheet = WizardAirportSheet(0, WizardAirportSheet.Side.to)
                assertTrue(model.airportIsAllowed(model.form.legs[0].fromIata, sheet))
                model.chooseAirport(wizardAirport("AMS", "Amsterdam", "Europe/Amsterdam"), sheet)
                assertEquals(listOf("AMS", "AMS"), model.form.route().map { it.iata })
                assertFalse(model.whereComplete)
                fixture.close()
            }
    }
}
