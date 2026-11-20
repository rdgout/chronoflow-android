@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.feature.wizard

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.inputFingerprint
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardStepKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import kotlin.time.Duration.Companion.seconds

internal fun flightDraft(
    home: Boolean = false,
    invalid: Boolean = false,
    sleep: Boolean = false,
): MemberWizardDraft {
    val route = routeDraft(home = home)
    val form =
        route.form
            .updateLeg(0) {
                it.copy(
                    departLocal = "2026-10-12T14:40",
                    arriveLocal = if (invalid) "2026-10-11T09:15" else "2026-10-13T09:15",
                )
            }.let {
                if (home) {
                    it.updateLeg(
                        1,
                    ) { leg -> leg.copy(departLocal = "2026-10-22T10:25", arriveLocal = "2026-10-22T15:30") }
                } else {
                    it
                }
            }
    return route
        .copy(
            form = form,
            state =
                route.state
                    .copy(
                        step = if (home) MemberWizardStep.returnFlight else MemberWizardStep.outbound,
                        airports =
                            listOf(
                                AMS,
                                NRT,
                                DXB,
                                BKK,
                            ).associateBy {
                                it.iata
                            },
                        sleepNeeded = sleep,
                        returnRouteConfirmed = home,
                    ),
        )
}

class TripWizardViewModelTimesTest {
    @get:Rule val main: org.junit.rules.TestWatcher = WizardMainDispatcherRule()

    @Test fun failedContinueKeepsTheStepAndReportsErrors() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { draft = flightDraft(invalid = true) }) { f ->
                runCurrent()
                assertEquals(
                    0,
                    f.model.uiState.value!!
                        .times!!
                        .continueFailures,
                )
                f.model.onEvent(WizardEvent.Continue(WizardStepKey.Outbound))
                runCurrent()
                assertEquals(
                    MemberWizardStep.outbound,
                    f.model.wizard!!
                        .draft.state.step,
                )
                assertNotNull(
                    f.model.wizard!!
                        .draft.fieldErrors["legs.0.arriveLocal"],
                )
                assertEquals(
                    1,
                    f.model.uiState.value!!
                        .times!!
                        .continueFailures,
                )
            }
        }

    @Test fun memberContinueGoesToComingBack() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { draft = flightDraft() }) { f ->
                runCurrent()
                f.model.onEvent(WizardEvent.Continue(WizardStepKey.Outbound))
                runCurrent()
                assertEquals(
                    MemberWizardStep.returnChoice,
                    f.model.wizard!!
                        .draft.state.step,
                )
            }
        }

    @Test fun revisitContinueFinishesFlightInput() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft =
                    flightDraft().let { it.copy(state = it.state.copy(revisit = it.inputFingerprint)) }
            }) { f ->
                runCurrent()
                f.model.onEvent(WizardEvent.Continue(WizardStepKey.Outbound))
                runCurrent()
                assertEquals(
                    MemberWizardStep.ready,
                    f.model.wizard!!
                        .draft.state.step,
                )
            }
        }

    @Test fun homeContinueFinishesFlightInput_sleep() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { draft = flightDraft(home = true, sleep = true) }) { f ->
                runCurrent()
                f.model.onEvent(WizardEvent.Continue(WizardStepKey.ReturnFlight))
                runCurrent()
                assertEquals(
                    MemberWizardStep.sleep,
                    f.model.wizard!!
                        .draft.state.step,
                )
            }
        }

    @Test fun homeContinueFinishesFlightInput_checking() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { draft = flightDraft(home = true) }) { f ->
                runCurrent()
                f.model.onEvent(WizardEvent.Continue(WizardStepKey.ReturnFlight))
                runCurrent()
                assertEquals(
                    MemberWizardStep.checking,
                    f.model.wizard!!
                        .draft.state.step,
                )
            }
        }

    @Test fun setDatePartAndSetTimePartEventsWriteTheWallValue() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft =
                    flightDraft().let { it.copy(form = it.form.updateLeg(0) { leg -> leg.copy(departLocal = "") }) }
            }) { f ->
                runCurrent()
                f.model.onEvent(WizardEvent.SetDatePart(0, false, LocalDate.of(2026, 10, 14)))
                f.model.onEvent(WizardEvent.SetTimePart(0, false, 7, 3))
                runCurrent()
                assertEquals(
                    "2026-10-14T07:03",
                    f.model.wizard!!
                        .draft.form.legs[0]
                        .departLocal,
                )
                f.model.confirmDiscard()
                f.model.onEvent(WizardEvent.SetDatePart(0, false, LocalDate.of(2026, 10, 15)))
                f.model.onEvent(WizardEvent.SetTimePart(0, false, 8, 4))
                assertEquals(
                    "2026-10-14T07:03",
                    f.model.wizard!!
                        .draft.form.legs[0]
                        .departLocal,
                )
            }
        }

    @Test fun timesUiCarriesTheAccountTimeFormat() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft = flightDraft()
                timeFormat = flowOf(TimeFormat.Value12)
            }) { f ->
                runCurrent()
                assertEquals(
                    TimeFormat.Value12,
                    f.model.uiState.value!!
                        .times!!
                        .timeFormat,
                )
            }
        }
}
