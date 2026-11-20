@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.wizard

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.contracts.PlanStatus
import net.chronoflow.core.testing.wizard.generationReview
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.WizardCheckingProgress
import net.chronoflow.core.wizard.model.WizardMode
import net.chronoflow.feature.wizard.checking.CheckFailureKind
import net.chronoflow.feature.wizard.checking.CheckLineState.Busy
import net.chronoflow.feature.wizard.checking.CheckLineState.Done
import net.chronoflow.feature.wizard.checking.CheckLineState.Open
import net.chronoflow.feature.wizard.checking.CheckingOutcome
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardStepKey
import net.chronoflow.feature.wizard.shell.WizardUiOverride
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class TripWizardViewModelCheckingTest {
    @get:Rule val main: org.junit.rules.TestWatcher = WizardMainDispatcherRule()

    @Test fun linesFollowTicks() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft = checkedDraft(MemberWizardStep.checking)
                source.configure(beforeReview = { delay(100) })
            }) { f ->
                runCurrent()

                fun lines() =
                    f.model.uiState.value!!
                        .checking!!
                        .lines
                        .map { it.second }
                assertEquals(listOf(Busy, Open, Open), lines())
                f.deps.checking.advance(399)
                assertEquals(listOf(Busy, Open, Open), lines())
                f.deps.checking.advance(400)
                assertEquals(listOf(Done, Busy, Open), lines())
                f.deps.checking.advance(800)
                assertEquals(listOf(Done, Done, Busy), lines())
                f.deps.checking.advance(1199)
                assertEquals(listOf(Done, Done, Busy), lines())
                f.deps.checking.advance(1200)
                assertEquals(listOf(Done, Done, Done), lines())
                f.deps.checking.advance(1499)
                assertEquals(
                    MemberWizardStep.checking,
                    f.model.uiState.value!!
                        .step.step,
                )
                f.deps.checking.advance(1500)
                assertNotEquals(
                    MemberWizardStep.checking,
                    f.model.uiState.value!!
                        .step.step,
                )
                assertEquals(
                    CheckingOutcome.Success,
                    f.model.uiState.value!!
                        .cue
                        ?.outcome,
                )
                assertEquals(
                    1,
                    f.model.uiState.value!!
                        .cue
                        ?.sequence,
                )
            }
        }

    @Test fun slowLineAtTenSeconds() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft = checkedDraft(MemberWizardStep.checking)
                source.configure(beforeReview = { CompletableDeferred<Unit>().await() })
            }) { f ->
                runCurrent()
                f.deps.checking.advance(9999)
                assertFalse(
                    f.model.uiState.value!!
                        .checking!!
                        .slow,
                )
                f.deps.checking.advance(10000)
                assertTrue(
                    f.model.uiState.value!!
                        .checking!!
                        .slow,
                )
            }
        }

    @Test fun serverFailureAtThirtySeconds() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft = checkedDraft(MemberWizardStep.checking)
                source.configure(beforeReview = { CompletableDeferred<Unit>().await() })
            }) { f ->
                runCurrent()
                f.deps.checking.advance(30000)
                val ui = f.model.uiState.value!!
                assertEquals(CheckFailureKind.Server, ui.checking!!.failure)
                assertEquals(0, ui.checking.retrySeconds)
                assertEquals("checking", ui.chrome.footerId)
                assertEquals(CheckingOutcome.Error, ui.cue?.outcome)
            }
        }

    @Test fun throttleCountsDownAndRetryWaits() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft = checkedDraft(MemberWizardStep.checking)
                source.configure(reviewFailure = DataFailure.Throttled(41.2, 429))
            }) { f ->
                runCurrent()
                assertEquals(
                    CheckFailureKind.Throttle,
                    f.model.uiState.value!!
                        .checking!!
                        .failure,
                )
                assertEquals(
                    42,
                    f.model.uiState.value!!
                        .checking!!
                        .retrySeconds,
                )
                f.deps.checking.advance(1000)
                assertEquals(
                    41,
                    f.model.uiState.value!!
                        .checking!!
                        .retrySeconds,
                )
                f.model.onEvent(WizardEvent.Continue(WizardStepKey.Checking))
                runCurrent()
                assertEquals(1, f.deps.source.count("review"))
                f.deps.checking.advance(41200)
                assertEquals(
                    0,
                    f.model.uiState.value!!
                        .checking!!
                        .retrySeconds,
                )
                f.deps.source.configure(beforeReview = { delay(100) })
                f.model.onEvent(WizardEvent.Continue(WizardStepKey.Checking))
                f.model.onEvent(WizardEvent.Continue(WizardStepKey.Checking))
                runCurrent()
                assertEquals(2, f.deps.source.count("review"))
            }
        }

    @Test fun offlineFailureMapsToOfflineCard() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft = checkedDraft(MemberWizardStep.checking)
                source.configure(reviewFailure = DataFailure.Offline)
            }) { f ->
                runCurrent()
                assertEquals(
                    CheckFailureKind.Offline,
                    f.model.uiState.value!!
                        .checking!!
                        .failure,
                )
                assertEquals(
                    CheckingOutcome.Error,
                    f.model.uiState.value!!
                        .cue
                        ?.outcome,
                )
            }
        }

    @Test fun validationFailureLeavesChecking() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft = checkedDraft(MemberWizardStep.checking)
                source.configure(
                    reviewFailure =
                        DataFailure.Validation(
                            mapOf("legs.0.toIata" to FieldMessage("Choose an airport from the search results.")),
                            422,
                        ),
                )
            }) { f ->
                runCurrent()
                assertEquals(
                    WizardStepKey.Where,
                    f.model.uiState.value!!
                        .step,
                )
                assertNull(
                    f.model.uiState.value!!
                        .checking,
                )
                assertNull(
                    f.model.uiState.value!!
                        .cue,
                )
            }
        }

    @Test fun backCancelsCheckingAndReturnsToTheLastInputStep() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft =
                    checkedDraft(MemberWizardStep.checking).let {
                        it.copy(state = it.state.copy(lastInputStep = MemberWizardStep.returnChoice))
                    }

                source.configure(beforeReview = { delay(1000) })
            }) { f ->
                runCurrent()
                f.deps.checking.advance(600)
                f.model.onEvent(WizardEvent.Back)
                runCurrent()
                assertEquals(
                    WizardStepKey.ReturnChoice,
                    f.model.uiState.value!!
                        .step,
                )
                f.deps.checking.advance(2000)
                assertEquals(
                    WizardStepKey.ReturnChoice,
                    f.model.uiState.value!!
                        .step,
                )
                assertNull(
                    f.model.uiState.value!!
                        .cue,
                )
            }
        }

    @Test fun hapticCuesOnCheckingEnd() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { draft = checkedDraft(MemberWizardStep.checking) }) { f ->
                f.deps.source.queueReviews(listOf(Result.success(generationReview().copy(status = PlanStatus.NoPlan))))
                runCurrent()
                f.deps.checking.advance(1500)
                assertEquals(
                    CheckingOutcome.Warning,
                    f.model.uiState.value!!
                        .cue
                        ?.outcome,
                )
                assertEquals(
                    1,
                    f.model.uiState.value!!
                        .cue
                        ?.sequence,
                )
            }
        }

    @Test fun closeOnFailureAsksOnlyWithChanges() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft =
                    checkedDraft(MemberWizardStep.checking).let { it.copy(state = it.state.copy(entered = true)) }

                source.configure(reviewFailure = DataFailure.Offline)
            }) { f ->
                runCurrent()
                f.model.onEvent(WizardEvent.RequestClose)
                runCurrent()
                assertTrue(f.model.discardDialogVisible.value)
            }
        }

    @Test fun guestClosesFailureDirectly() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft =
                    checkedDraft(MemberWizardStep.checking).let {
                        it.copy(state = it.state.copy(mode = WizardMode.guest, entered = true, sleepCompleted = true))
                    }

                source.configure(reviewFailure = DataFailure.Offline)
            }) { f ->
                runCurrent()
                f.model.onEvent(WizardEvent.RequestClose)
                runCurrent()
                assertFalse(f.model.discardDialogVisible.value)
                assertNotNull(f.model.exit.value)
            }
        }

    @Test fun uiOverrideFreezesCheckingWithoutARequest() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft = checkedDraft(MemberWizardStep.checking)
                uiOverride =
                    WizardUiOverride(WizardCheckingProgress(2, slow = true))
            }) { f ->
                runCurrent()
                assertEquals(
                    listOf(Done, Done, Busy),
                    f.model.uiState.value!!
                        .checking!!
                        .lines
                        .map { it.second },
                )
                assertTrue(
                    f.model.uiState.value!!
                        .checking!!
                        .slow,
                )
                assertEquals(0, f.deps.source.count("review"))
            }
        }
}
