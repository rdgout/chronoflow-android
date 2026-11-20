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
            withWizard(configure = { draft = freeDayDraft(freeDayPrompt(kind = PromptKind.SeatSide)) }) { fixture ->
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
}
