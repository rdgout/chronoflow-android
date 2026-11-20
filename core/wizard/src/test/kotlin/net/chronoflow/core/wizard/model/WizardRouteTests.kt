@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.core.wizard.model

import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.wizard.generationInput
import net.chronoflow.core.wizard.wizardAirport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.seconds

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

    @org.junit.runner.RunWith(org.junit.runners.Parameterized::class)
    class MixedErrors(
        private val paths: List<String>,
        private val step: MemberWizardStep,
    ) {
        @Test fun mixedServerErrorsReturnToTheEarliestScreenInPathOrder() {
            val form =
                WizardForm
                    .from(generationInput())
                    .insertStop(wizardAirport("DXB", "Dubai", "Asia/Dubai"), 0)
                    .addReturn()
                    .insertStop(wizardAirport("HEL", "Helsinki", "Europe/Helsinki"), 2)
            val errors =
                paths.associateWith {
                    net.chronoflow.core.model
                        .FieldMessage("error")
                }
            assertEquals(step, WizardJourney.errorStep(errors, form))
        }

        companion object {
            @JvmStatic
            @org.junit.runners.Parameterized.Parameters(name = "paths={0}")
            fun cases() =
                listOf(
                    arrayOf<Any>(listOf("legs.0.arriveLocal", "legs.2.toIata"), MemberWizardStep.outbound),
                    arrayOf<Any>(listOf("legs.1.departLocal", "legs.2.fromIata"), MemberWizardStep.outbound),
                    arrayOf<Any>(listOf("legs.1.toIata", "legs.2.departLocal"), MemberWizardStep.whereTo),
                    arrayOf<Any>(
                        listOf("legs.0.toIata", "legs.2.toIata", "legs.1.departLocal"),
                        MemberWizardStep.whereTo,
                    ),
                    arrayOf<Any>(listOf("legs.2.toIata", "legs.3.arriveLocal"), MemberWizardStep.returnRoute),
                    arrayOf<Any>(listOf("legs.2.departLocal", "legs.3.arriveLocal"), MemberWizardStep.returnFlight),
                )
        }
    }

    @org.junit.runner.RunWith(org.junit.runners.Parameterized::class)
    class EditorErrors(
        private val path: String,
        private val step: MemberWizardStep,
    ) {
        @Test fun laterCardOwnsChronologyErrorAndServerFieldsReturnToTheirEditors() {
            var form =
                WizardForm
                    .from(generationInput())
                    .insertStop(wizardAirport("DXB", "Dubai", "Asia/Dubai"), 0)
                    .updateLeg(0) { it.copy(arriveLocal = "2026-11-11T12:00") }
                    .updateLeg(1) { it.copy(departLocal = "2026-11-11T11:00") }
            val errors = WizardValidation.validate(form, mapOf("DXB" to wizardAirport("DXB", "Dubai", "Asia/Dubai")))
            assertEquals("This flight leaves before the previous one lands.", errors["legs.1.departLocal"]?.key)
            org.junit.Assert.assertNull(errors["legs.0.departLocal"])
            form = form.addReturn()
            assertEquals(
                step,
                WizardJourney.errorStep(
                    mapOf(
                        path to
                            net.chronoflow.core.model
                                .FieldMessage("error"),
                    ),
                    form,
                ),
            )
            val rejection =
                net.chronoflow.core.model.FieldMessage(
                    "Your flight home must leave from the airport you landed at.",
                )
            val mapped = WizardJourney.errors(mapOf("return.legs.0.from_iata" to rejection), form)
            assertEquals(mapOf("review" to rejection), mapped)
            assertEquals(MemberWizardStep.returnFlight, WizardJourney.errorStep(mapped, form))
        }

        companion object {
            @JvmStatic
            @org.junit.runners.Parameterized.Parameters(name = "path={0}")
            fun cases() =
                listOf(
                    arrayOf<Any>("legs.1.departLocal", MemberWizardStep.outbound),
                    arrayOf<Any>("legs.1.toIata", MemberWizardStep.whereTo),
                    arrayOf<Any>("legs.2.departLocal", MemberWizardStep.returnFlight),
                    arrayOf<Any>("legs.2.toIata", MemberWizardStep.returnRoute),
                    arrayOf<Any>("review", MemberWizardStep.returnFlight),
                )
        }
    }

    @org.junit.runner.RunWith(org.junit.runners.Parameterized::class)
    class Recovery(
        private val confirmed: Boolean,
    ) {
        @Test fun recoveryDecodesOldReturnFlightAndPreservesConfirmedTimes() {
            val initial = MemberWizardDrafts.memberInitial(false, null, null, null)
            val draft =
                initial.copy(
                    form = WizardForm.from(generationInput()).addReturn(),
                    state =
                        initial.state.copy(
                            step = MemberWizardStep.returnFlight,
                            returnRouteConfirmed = confirmed.takeIf { it },
                        ),
                )
            val recovered = MemberWizardRecord.decode(MemberWizardRecord.encode(draft))!!
            assertEquals(
                if (confirmed) MemberWizardStep.returnFlight else MemberWizardStep.returnRoute,
                recovered.state.step,
            )
            assertEquals(draft.form, recovered.form)
        }

        companion object {
            @JvmStatic
            @org.junit.runners.Parameterized.Parameters(name = "confirmed={0}")
            fun cases() = listOf(false, true)
        }
    }

    class ModelRoutes {
        @Test fun returnChoiceStartsWithADirectRouteAndEmptyTimes() =
            runTest(timeout = 60.seconds) {
                val fixture = WizardModelFixture(backgroundScope, testScheduler)
                val model = fixture.model
                model.form =
                    model.form
                        .insertStop(wizardAirport("DXB", "Dubai", "Asia/Dubai"), 0)
                        .insertStop(wizardAirport("HEL", "Helsinki", "Europe/Helsinki"), 1)
                model.state = model.state.copy(step = MemberWizardStep.returnChoice)
                val before = model.form
                model.chooseReturn("return_legs")
                runCurrent()
                assertTrue(fixture.checking.deadlines.contains(250))
                fixture.checking.advance(250)
                assertEquals(MemberWizardStep.returnRoute, model.state.step)
                assertEquals(
                    listOf(before.legs.last().toIata, before.legs[0].fromIata),
                    model.form.route(true).map { it.iata },
                )
                assertEquals(4, model.form.legs.size)
                assertTrue(
                    model.form.legs
                        .last()
                        .departLocal
                        .isEmpty() &&
                        model.form.legs
                            .last()
                            .arriveLocal
                            .isEmpty(),
                )
                assertEquals(false, model.state.returnRouteConfirmed)
                model.finishReturnRoute()
                assertEquals(MemberWizardStep.returnFlight, model.state.step)
                val sheet = WizardAirportSheet(0, WizardAirportSheet.Side.stop, true)
                model.openAirport(sheet)
                model.chooseAirport(wizardAirport("LHR", "London", "Europe/London"), sheet)
                assertEquals(4, model.form.legs.size)
                assertEquals("LHR", model.form.legs[0].toIata)
                assertEquals("LHR", model.form.legs[1].fromIata)
                model.flush()
                assertEquals(
                    MemberWizardStep.returnFlight,
                    fixture.store.last
                        ?.state
                        ?.step,
                )
                fixture.close()
            }

        @Test fun routeEditingLocksAndFollowsHomeAndBackKeepsStops() =
            runTest(timeout = 60.seconds) {
                val fixture = WizardModelFixture(backgroundScope, testScheduler)
                val model = fixture.model
                model.form = model.form.addReturn().setRouteAirport(wizardAirport("AMS", "Origin", "UTC"), 1, true)
                val stop = wizardAirport("DXB", "Dubai", "Asia/Dubai")
                val second = wizardAirport("HEL", "Helsinki", "Europe/Helsinki")
                model.chooseAirport(stop, WizardAirportSheet(0, WizardAirportSheet.Side.stop))
                val route = model.form.route()
                model.swapAirports()
                assertEquals(route, model.form.route())
                model.go(MemberWizardStep.outbound)
                model.popNavigation(emptyList(), listOf(MemberWizardDestination.outbound))
                assertEquals(MemberWizardStep.whereTo, model.state.step)
                assertEquals(route, model.form.route())
                model.chooseAirport(second, WizardAirportSheet(0, WizardAirportSheet.Side.from))
                assertEquals(
                    "HEL",
                    model.form.legs
                        .last()
                        .toIata,
                )
                model.chooseAirport(second, WizardAirportSheet(1, WizardAirportSheet.Side.to))
                assertEquals("HEL", model.form.legs[2].fromIata)
                model.chooseAirport(stop, WizardAirportSheet(2, WizardAirportSheet.Side.from))
                assertEquals("HEL", model.form.legs[2].fromIata)
                model.chooseAirport(stop, WizardAirportSheet(2, WizardAirportSheet.Side.home))
                model.chooseAirport(
                    wizardAirport("LHR", "London", "Europe/London"),
                    WizardAirportSheet(0, WizardAirportSheet.Side.from),
                )
                assertEquals(
                    "DXB",
                    model.form.legs
                        .last()
                        .toIata,
                )
                model.go(MemberWizardStep.returnRoute)
                model.finishReturnRoute()
                assertEquals(true, model.state.returnRouteConfirmed)
                assertEquals(MemberWizardStep.returnFlight, model.state.step)
                assertEquals(
                    listOf(
                        MemberWizardDestination.outbound,
                        MemberWizardDestination.returnChoice,
                        MemberWizardDestination.returnRoute,
                        MemberWizardDestination.returnFlight,
                    ),
                    model.navigationPath,
                )
                val path = model.navigationPath
                model.popNavigation(path.dropLast(1), path)
                assertEquals(MemberWizardStep.returnRoute, model.state.step)
                model.makeDirect()
                model.swapAirports()
                assertEquals(listOf("HEL", "LHR"), model.form.route().map { it.iata })
                model.flush()
                assertEquals(model.draft, fixture.store.last)
                fixture.close()
            }
    }
}
