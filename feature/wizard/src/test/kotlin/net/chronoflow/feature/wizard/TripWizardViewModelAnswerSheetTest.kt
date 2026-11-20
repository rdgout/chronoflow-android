@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.wizard

import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.contracts.PromptKind
import net.chronoflow.core.model.contracts.ReviewPromptTarget
import net.chronoflow.feature.wizard.shell.WizardEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class TripWizardViewModelAnswerSheetTest {
    @get:Rule val main: org.junit.rules.TestWatcher = WizardMainDispatcherRule()

    @Test fun restoredPromptShowsFreshLoadValues() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { draft = freeDayDraft() }) { fixture ->
                runCurrent()
                val sheet =
                    fixture.model.uiState.value!!
                        .answerSheet!!
                assertEquals("free_day_times::", sheet.state.prompt.id)
                assertEquals(JsonPrimitive("00:30"), sheet.state.values["free_day_bed"])
            }
        }

    @Test fun sameAsWorkdaysHidesTimesAndDoneAppliesTheAnswer() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { draft = freeDayDraft() }) { fixture ->
                runCurrent()
                fixture.model.onEvent(WizardEvent.UpdateAnswer("free_day_status", JsonPrimitive("same_as_workdays")))
                runCurrent()
                assertFalse(
                    fixture.model.uiState.value!!
                        .answerSheet!!
                        .declared,
                )
                fixture.model.onEvent(WizardEvent.AnswerDone)
                runCurrent()
                assertNull(
                    fixture.model.wizard!!
                        .draft.state.promptSheetID,
                )
                assertEquals(
                    JsonPrimitive("same_as_workdays"),
                    fixture.model.wizard!!
                        .draft.state.profile["free_day_status"],
                )
                assertNull(
                    fixture.model.uiState.value!!
                        .answerSheet,
                )
            }
        }

    @Test fun missingTimeStaysWithAnErrorAndFailureCount() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { draft = freeDayDraft() }) { fixture ->
                runCurrent()
                fixture.model.onEvent(WizardEvent.UpdateAnswer("free_day_bed", JsonNull))
                fixture.model.onEvent(WizardEvent.AnswerDone)
                runCurrent()
                val sheet =
                    fixture.model.uiState.value!!
                        .answerSheet!!
                assertEquals("Check your bedtime and wake-up time.", sheet.state.error?.key)
                assertEquals(1, sheet.state.failures)
            }
        }

    @Test fun staleTripLegMapsReviewExpiredToAnError() =
        runTest(timeout = 60.seconds) {
            val path = "outbound.legs.99.free_day_status"
            val prompt = freeDayPrompt(target = ReviewPromptTarget.Trip, fields = listOf(path))
            withWizard(configure = { draft = freeDayDraft(prompt) }) { fixture ->
                runCurrent()
                fixture.model.onEvent(WizardEvent.UpdateAnswer(path, JsonPrimitive("same_as_workdays")))
                fixture.model.onEvent(WizardEvent.AnswerDone)
                runCurrent()
                assertEquals(
                    "Choose one of the available options.",
                    fixture.model.uiState.value!!
                        .answerSheet!!
                        .state.error
                        ?.key,
                )
                assertNotNull(
                    fixture.model.wizard!!
                        .draft.state.promptSheetID,
                )
            }
        }

    @Test fun invalidTargetMapsValidationToAnError() =
        runTest(timeout = 60.seconds) {
            val prompt = freeDayPrompt(target = ReviewPromptTarget.Unknown("stale"))
            withWizard(configure = { draft = freeDayDraft(prompt) }) { fixture ->
                runCurrent()
                fixture.model.onEvent(WizardEvent.UpdateAnswer("free_day_status", JsonPrimitive("same_as_workdays")))
                fixture.model.onEvent(WizardEvent.AnswerDone)
                runCurrent()
                assertEquals(
                    "Choose one of the available options.",
                    fixture.model.uiState.value!!
                        .answerSheet!!
                        .state.error
                        ?.key,
                )
            }
        }

    @Test fun malformedAnswerMapsTheSerializationFailureToAnError() =
        runTest(timeout = 60.seconds) {
            val path = "outbound.legs"
            val prompt = freeDayPrompt(target = ReviewPromptTarget.Trip, fields = listOf(path))
            withWizard(configure = { draft = freeDayDraft(prompt) }) { fixture ->
                runCurrent()
                fixture.model.onEvent(WizardEvent.UpdateAnswer(path, JsonPrimitive("invalid")))
                fixture.model.onEvent(WizardEvent.AnswerDone)
                runCurrent()
                assertEquals(
                    "Choose one of the available options.",
                    fixture.model.uiState.value!!
                        .answerSheet!!
                        .state.error
                        ?.key,
                )
                assertNotNull(
                    fixture.model.wizard!!
                        .draft.state.promptSheetID,
                )
            }
        }

    @Test fun cancelLeavesTheProfileUnchanged() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { draft = freeDayDraft() }) { fixture ->
                runCurrent()
                val before =
                    fixture.model.wizard!!
                        .draft.state.profile
                fixture.model.onEvent(WizardEvent.UpdateAnswer("free_day_status", JsonPrimitive("same_as_workdays")))
                fixture.model.onEvent(WizardEvent.CancelAnswer)
                runCurrent()
                assertNull(
                    fixture.model.uiState.value!!
                        .answerSheet,
                )
                assertEquals(
                    before,
                    fixture.model.wizard!!
                        .draft.state.profile,
                )
            }
        }

    @Test fun confirmFreeDayTimeRoundsWithoutWritingTheProfile() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { draft = freeDayDraft() }) { fixture ->
                runCurrent()
                fixture.model.onEvent(WizardEvent.OpenTimeSheet("free_day_bed"))
                fixture.model.onEvent(WizardEvent.ConfirmTimeSheet("free_day_bed", 0, 42, true))
                runCurrent()
                val sheet =
                    fixture.model.uiState.value!!
                        .answerSheet!!
                assertEquals(JsonPrimitive("00:40"), sheet.state.values["free_day_bed"])
                assertNull(sheet.openTime)
                assertFalse(
                    "free_day_bed" in
                        fixture.model.wizard!!
                            .draft.state.profile,
                )
            }
        }

    @Test fun unsupportedKindHasNoSheet() =
        runTest(timeout = 60.seconds) {
            withWizard(
                configure = { draft = freeDayDraft(freeDayPrompt(kind = PromptKind.Unknown("unsupported"))) },
            ) { fixture ->
                runCurrent()
                assertNull(
                    fixture.model.uiState.value!!
                        .answerSheet,
                )
            }
        }

    @Test fun unknownPromptHasNoSheet() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                val base = freeDayDraft()
                draft = base.copy(state = base.state.copy(promptSheetID = "missing"))
            }) { fixture ->
                runCurrent()
                assertNull(
                    fixture.model.uiState.value!!
                        .answerSheet,
                )
            }
        }

    @Test fun seatOpensFromOpenPrompt() = assertOpens(PromptKind.SeatSide)

    @Test fun roomOpensFromOpenPrompt() = assertOpens(PromptKind.Room)

    @Test fun checkoutOpensFromOpenPrompt() = assertOpens(PromptKind.LateCheckOut)

    @Test fun wakeOpensFromOpenPrompt() = assertOpens(PromptKind.LatestWake)

    @Test fun busyOpensFromOpenPrompt() = assertOpens(PromptKind.PreDepartureBusyHours)

    @Test fun drinksOpensFromOpenPrompt() = assertOpens(PromptKind.Cups)

    @Test fun freeDaysOpenFromOpenPrompt() = assertOpens(PromptKind.FreeDayTimes)

    @Test fun lightOpensFromOpenPrompt() = assertOpens(PromptKind.LightDevice)

    @Test fun blueOpensFromOpenPrompt() = assertOpens(PromptKind.BlueBlockers)

    private fun assertOpens(kind: PromptKind) =
        runTest(timeout = 60.seconds) {
            val prompt = completionPrompt(kind)
            withWizard(configure = {
                val base = freeDayDraft(prompt)
                draft = base.copy(state = base.state.copy(promptSheetID = null))
            }) { f ->
                runCurrent()
                assertNull(
                    f.model.uiState.value!!
                        .answerSheet,
                )
                f.model.onEvent(WizardEvent.OpenPrompt(prompt.id))
                runCurrent()
                assertEquals(
                    kind,
                    f.model.uiState.value!!
                        .answerSheet!!
                        .state.prompt.kind,
                )
            }
        }

    @Test fun foreignAnswerFieldIsIgnored() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { draft = freeDayDraft(completionPrompt(PromptKind.SeatSide)) }) { f ->
                runCurrent()
                val before =
                    f.model.uiState.value!!
                        .answerSheet!!
                        .state
                f.model.onEvent(WizardEvent.UpdateAnswer("foreign", JsonPrimitive(1)))
                runCurrent()
                assertEquals(
                    before,
                    f.model.uiState.value!!
                        .answerSheet!!
                        .state,
                )
            }
        }

    @Test fun invalidBlueAddsOneFailureWithoutWriting() = assertInvalidAnswer(PromptKind.BlueBlockers)

    @Test fun invalidDrinksAddOneFailureWithoutWriting() = assertInvalidAnswer(PromptKind.Cups)

    private fun assertInvalidAnswer(kind: PromptKind) =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { draft = freeDayDraft(completionPrompt(kind)) }) { f ->
                runCurrent()
                val before = f.model.wizard!!.draft
                if (kind == PromptKind.BlueBlockers) {
                    f.model.onEvent(WizardEvent.UpdateAnswer("blue_blockers", JsonPrimitive("rated")))
                    f.model.onEvent(WizardEvent.UpdateAnswer("blue_blocker_mdfd", JsonPrimitive("12")))
                } else {
                    f.model.onEvent(WizardEvent.UpdateAnswer("cups_filter_coffee", JsonPrimitive(0)))
                }
                f.model.onEvent(WizardEvent.AnswerDone)
                runCurrent()
                assertEquals(
                    1,
                    f.model.uiState.value!!
                        .answerSheet!!
                        .state.failures,
                )
                assertNotNull(
                    f.model.uiState.value!!
                        .answerSheet!!
                        .state.error,
                )
                assertEquals(before, f.model.wizard!!.draft)
            }
        }

    @Test fun aisleWritesNullAndShowsAdd() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft =
                    fineTuneDraft().let { it.copy(state = it.state.copy(promptSheetID = "seat_side:outbound:0")) }
            }) { f ->
                runCurrent()
                f.model.onEvent(WizardEvent.UpdateAnswer("seat_window", JsonNull))
                f.model.onEvent(WizardEvent.AnswerDone)
                runCurrent()
                assertNull(
                    f.model.wizard!!
                        .draft.form.legs[0]
                        .seatWindow,
                )
                assertNull(
                    f.model.uiState.value!!
                        .fineTune!!
                        .rows
                        .first { it.kind == PromptKind.SeatSide }
                        .answer,
                )
            }
        }

    @Test fun cancelAnswerWritesNothing() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft =
                    fineTuneDraft().let { it.copy(state = it.state.copy(promptSheetID = "seat_side:outbound:0")) }
            }) { f ->
                runCurrent()
                val before =
                    f.model.wizard!!
                        .draft.form
                f.model.onEvent(WizardEvent.UpdateAnswer("seat_window", JsonPrimitive("right")))
                f.model.onEvent(WizardEvent.CancelAnswer)
                runCurrent()
                assertEquals(
                    before,
                    f.model.wizard!!
                        .draft.form,
                )
            }
        }
}
