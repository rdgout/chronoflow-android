@file:Suppress("ktlint:standard:max-line-length")

@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.wizard

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.contracts.PromptKind
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.WizardSaveFailure
import net.chronoflow.feature.wizard.checking.CheckingOutcome
import net.chronoflow.feature.wizard.ready.ReadyButtonLabel
import net.chronoflow.feature.wizard.ready.ReadyFooterUi
import net.chronoflow.feature.wizard.ready.ReadyRow
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardExit
import net.chronoflow.feature.wizard.shell.WizardStepKey
import net.chronoflow.feature.wizard.shell.WizardUiOverride
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class TripWizardViewModelReadyTest {
    @get:Rule val main: org.junit.rules.TestWatcher = WizardMainDispatcherRule()

    private suspend fun TestScope.withReady(block: suspend TestScope.(WizardFixture) -> Unit) =
        withWizard(configure = {
            val base = checkedDraft(MemberWizardStep.fineTune)
            draft =
                base.copy(
                    state =
                        base.state.copy(
                            review =
                                base.state.review!!.copy(
                                    prompts = listOf(completionPrompt(PromptKind.LatestWake)),
                                ),
                        ),
                )
        }) { f ->
            runCurrent()
            f.model.onEvent(WizardEvent.Continue(WizardStepKey.FineTune))
            runCurrent()
            assertEquals(
                WizardStepKey.Ready,
                f.model.uiState.value!!
                    .step,
            )
            block(f)
        }

    private fun checkRow(
        row: ReadyRow,
        step: MemberWizardStep,
    ) = runTest(timeout = 60.seconds) {
        withReady { f ->
            f.model.onEvent(WizardEvent.RevisitFromReady(row))
            runCurrent()
            assertEquals(
                step,
                f.model.uiState.value!!
                    .step.step,
            )
            if (row == ReadyRow.Sleep) {
                assertTrue(
                    f.model.wizard!!
                        .draft.form.override.enabled,
                )
            }
            if (row ==
                ReadyRow.LatestWake
            ) {
                assertEquals(
                    completionPrompt(PromptKind.LatestWake).id,
                    f.model.wizard!!
                        .draft.state.promptSheetID,
                )
            }
        }
    }

    @Test fun routeRevisitsWhere() = checkRow(ReadyRow.Route, MemberWizardStep.whereTo)

    @Test fun flightRevisitsOutbound() = checkRow(ReadyRow.Flight, MemberWizardStep.outbound)

    @Test fun returnRevisitsChoice() = checkRow(ReadyRow.Return, MemberWizardStep.returnChoice)

    @Test fun sleepRevisitsFineTuneWithOverride() = checkRow(ReadyRow.Sleep, MemberWizardStep.fineTune)

    @Test fun extrasRevisitFineTune() = checkRow(ReadyRow.Extras, MemberWizardStep.fineTune)

    @Test fun latestWakeRevisitsAndOpensPrompt() = checkRow(ReadyRow.LatestWake, MemberWizardStep.fineTune)

    @Test fun foregroundDoesNotForceAnAffordableReview() =
        runTest(timeout = 60.seconds) {
            withReady { f ->
                f.model.onEvent(WizardEvent.ReadyForeground)
                runCurrent()
                assertEquals(0, f.deps.source.count("review"))
            }
        }

    @Test fun foregroundForcesUnavailable() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                val base = checkedDraft(MemberWizardStep.fineTune)
                val review = base.state.review!!
                draft =
                    base.copy(
                        state =
                            base.state.copy(
                                review =
                                    review.copy(
                                        canSave = false,
                                        saveBlockedReason = "Not enough credits",
                                        price = review.price!!.copy(balance = 0),
                                    ),
                            ),
                    )
            }) { f ->
                runCurrent()
                f.model.onEvent(WizardEvent.Continue(WizardStepKey.FineTune))
                runCurrent()
                f.model.onEvent(WizardEvent.ReadyTick)
                runCurrent()
                assertEquals(0, f.deps.source.count("review"))
                f.model.onEvent(WizardEvent.ReadyForeground)
                runCurrent()
                assertEquals(1, f.deps.source.count("review"))
            }
        }

    @Test fun frozenTickAndForegroundNeverReviewAndBusyUsesOverride() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft = checkedDraft(MemberWizardStep.ready)
                uiOverride =
                    WizardUiOverride(reviewing = true)
            }) { f ->
                runCurrent()
                f.model.onEvent(WizardEvent.ReadyTick)
                f.model.onEvent(WizardEvent.ReadyForeground)
                runCurrent()
                assertEquals(0, f.deps.source.count("review"))
                assertEquals(
                    ReadyFooterUi.Primary(ReadyButtonLabel.Checking, false, true),
                    f.model.uiState.value!!
                        .ready!!
                        .footer,
                )
                assertTrue(
                    f.model.uiState.value!!
                        .chrome.primaryBusy,
                )
            }
        }

    @Test fun frozenCountdownUsesOverride() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                val base = checkedDraft(MemberWizardStep.ready)
                draft =
                    base.copy(state = base.state.copy(saveFailure = WizardSaveFailure(WizardSaveFailure.Kind.throttle)))
                uiOverride = WizardUiOverride(retrySeconds = 42)
            }) { f ->
                runCurrent()
                assertEquals(
                    ReadyFooterUi.Primary(ReadyButtonLabel.Countdown(42), false, false),
                    f.model.uiState.value!!
                        .ready!!
                        .footer,
                )
                assertFalse(
                    f.model.uiState.value!!
                        .chrome.continueEnabled,
                )
            }
        }

    @Test fun failureChangesEmitOneErrorCueEach() =
        runTest(timeout = 60.seconds) {
            withReady { f ->
                assertNull(
                    f.model.uiState.value!!
                        .cue,
                )
                f.deps.source.configure(saveFailure = net.chronoflow.core.model.DataFailure.Offline)
                f.model.onEvent(WizardEvent.Continue(WizardStepKey.Ready))
                runCurrent()
                assertEquals(
                    CheckingOutcome.Error,
                    f.model.uiState.value!!
                        .cue!!
                        .outcome,
                )
                val sequence =
                    f.model.uiState.value!!
                        .cue!!
                        .sequence
                f.deps.source.configure(
                    saveFailure =
                        net.chronoflow.core.model.DataFailure
                            .Server(500),
                )
                f.model.onEvent(WizardEvent.Continue(WizardStepKey.Ready))
                runCurrent()
                assertEquals(
                    sequence + 1,
                    f.model.uiState.value!!
                        .cue!!
                        .sequence,
                )
            }
        }

    @Test fun recoveredFailureDoesNotBuzz() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                val base = checkedDraft(MemberWizardStep.ready)
                draft =
                    base.copy(state = base.state.copy(saveFailure = WizardSaveFailure(WizardSaveFailure.Kind.offline)))
                uiOverride = WizardUiOverride()
            }) { f ->
                runCurrent()
                assertNull(
                    f.model.uiState.value!!
                        .cue,
                )
            }
        }

    @Test fun savedExitKeepsTheTripId() =
        runTest(timeout = 60.seconds) {
            withReady { f ->
                f.model.onEvent(WizardEvent.Continue(WizardStepKey.Ready))
                runCurrent()
                assertEquals(WizardExit.Saved("created"), f.model.exit.value)
                assertTrue(f.model.state.value is TripWizardUiState.Closed)
            }
        }
}
