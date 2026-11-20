@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.wizard

import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.feature.wizard.shell.WizardEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class TripWizardViewModelReturnChoiceTest {
    @get:Rule val main: org.junit.rules.TestWatcher = WizardMainDispatcherRule()

    @Test fun webChoiceKeepsTheWebEnd() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft = webEndDraft().let { it.copy(state = it.state.copy(step = MemberWizardStep.returnChoice)) }
            }) { f ->
                runCurrent()
                f.model.onEvent(WizardEvent.ChooseReturn("nights"))
                runCurrent()
                f.deps.checking.advance(249)
                assertEquals(
                    MemberWizardStep.returnChoice,
                    f.model.wizard!!
                        .draft.state.step,
                )
                f.deps.checking.advance(250)
                assertEquals(
                    "nights",
                    f.model.wizard!!
                        .draft.form.end.kind,
                )
                assertEquals(
                    9,
                    f.model.wizard!!
                        .draft.form.end.nights,
                )
                assertTrue(
                    f.model.wizard!!
                        .draft.state.step != MemberWizardStep.returnChoice,
                )
            }
        }

    @Test fun oneWayAdvancesAfter250ms() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft =
                    flightDraft().let { it.copy(state = it.state.copy(step = MemberWizardStep.returnChoice)) }
            }) { f ->
                runCurrent()
                f.model.onEvent(WizardEvent.ChooseReturn("one_way"))
                runCurrent()
                f.deps.checking.advance(249)
                assertEquals(
                    MemberWizardStep.returnChoice,
                    f.model.wizard!!
                        .draft.state.step,
                )
                assertTrue(f.model.wizard!!.choicePending)
                f.deps.checking.advance(250)
                assertEquals(
                    MemberWizardStep.checking,
                    f.model.wizard!!
                        .draft.state.step,
                )
                assertFalse(f.model.wizard!!.choicePending)
                assertEquals(1, f.deps.source.reviewInputs.size)
            }
        }

    @Test fun oneWayAdvancesAfter250msToSleepWhenNeeded() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft =
                    flightDraft(
                        sleep = true,
                    ).let { it.copy(state = it.state.copy(step = MemberWizardStep.returnChoice)) }
            }) { f ->
                runCurrent()
                f.model.onEvent(WizardEvent.ChooseReturn("one_way"))
                runCurrent()
                f.deps.checking.advance(250)
                assertEquals(
                    MemberWizardStep.sleep,
                    f.model.wizard!!
                        .draft.state.step,
                )
            }
        }

    @Test fun secondChoiceDuringThePauseIsIgnored() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft =
                    flightDraft().let { it.copy(state = it.state.copy(step = MemberWizardStep.returnChoice)) }
            }) { f ->
                runCurrent()
                f.model.onEvent(WizardEvent.ChooseReturn("one_way"))
                runCurrent()
                f.deps.checking.advance(100)
                f.model.onEvent(WizardEvent.ChooseReturn("return_legs"))
                runCurrent()
                assertEquals(
                    "one_way",
                    f.model.wizard!!
                        .draft.form.end.kind,
                )
                f.deps.checking.advance(250)
                assertEquals(
                    MemberWizardStep.checking,
                    f.model.wizard!!
                        .draft.state.step,
                )
                assertEquals(1, f.deps.source.reviewInputs.size)
            }
        }

    @Test fun addReturnStartsTheHomeRouteFromTheLastArrivalToTheOrigin() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft =
                    flightDraft().let { it.copy(state = it.state.copy(step = MemberWizardStep.returnChoice)) }
            }) { f ->
                runCurrent()
                f.model.onEvent(WizardEvent.ChooseReturn("return_legs"))
                runCurrent()
                f.deps.checking.advance(250)
                val draft = f.model.wizard!!.draft
                assertEquals(MemberWizardStep.returnRoute, draft.state.step)
                assertEquals("NRT", draft.form.legs[1].fromIata)
                assertEquals("AMS", draft.form.legs[1].toIata)
                assertEquals(false, draft.state.returnRouteConfirmed)
                assertFalse(draft.state.homeChanged)
            }
        }

    @Test fun backDuringThePauseCancelsTheChoice() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft =
                    flightDraft().let { it.copy(state = it.state.copy(step = MemberWizardStep.returnChoice)) }
            }) { f ->
                runCurrent()
                f.model.onEvent(WizardEvent.ChooseReturn("one_way"))
                runCurrent()
                f.deps.checking.advance(100)
                f.model.onEvent(WizardEvent.Back)
                runCurrent()
                assertEquals(
                    MemberWizardStep.outbound,
                    f.model.wizard!!
                        .draft.state.step,
                )
                f.deps.checking.advance(250)
                assertEquals(
                    MemberWizardStep.outbound,
                    f.model.wizard!!
                        .draft.state.step,
                )
                assertFalse(f.model.wizard!!.choicePending)
                assertEquals(0, f.deps.source.reviewInputs.size)
            }
        }

    @Test fun addReturnIsInertAtSixteenFlights() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft =
                    routeDraft(stops = 15).let { it.copy(state = it.state.copy(step = MemberWizardStep.returnChoice)) }
            }) { f ->
                runCurrent()
                val before = f.model.wizard!!.draft
                f.model.onEvent(WizardEvent.ChooseReturn("return_legs"))
                runCurrent()
                assertEquals(before, f.model.wizard!!.draft)
                assertFalse(f.model.wizard!!.choicePending)
                assertEquals(0, f.deps.checking.waiterCount)
            }
        }
}
