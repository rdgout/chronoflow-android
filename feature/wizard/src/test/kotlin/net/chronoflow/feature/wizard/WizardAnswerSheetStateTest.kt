package net.chronoflow.feature.wizard

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.contracts.ApiReviewIdentity
import net.chronoflow.core.model.contracts.ApiReviewItinerary
import net.chronoflow.core.model.contracts.ApiReviewPrompt
import net.chronoflow.core.model.contracts.ApiReviewPromptScope
import net.chronoflow.core.model.contracts.ApiReviewPromptShape
import net.chronoflow.core.model.contracts.ApiTripReview
import net.chronoflow.core.model.contracts.CopyLine
import net.chronoflow.core.model.contracts.PlanStatus
import net.chronoflow.core.model.contracts.PromptKind
import net.chronoflow.core.model.contracts.ReviewAnswerShape
import net.chronoflow.core.model.contracts.ReviewPromptTarget
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.WizardOptionalAnswer
import net.chronoflow.core.wizard.model.reviewInput
import net.chronoflow.feature.wizard.answers.WizardAnswerSheetState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

internal fun freeDayPrompt(
    kind: PromptKind = PromptKind.FreeDayTimes,
    target: ReviewPromptTarget = ReviewPromptTarget.Profile,
    fields: List<String> = listOf("free_day_status", "free_day_bed", "free_day_wake"),
    answer: Map<String, JsonPrimitive> = emptyMap(),
) = ApiReviewPrompt(
    "free_day_times::",
    kind,
    false,
    false,
    CopyLine("What time do you go to bed and wake up on free days?", emptyList()),
    ApiReviewPromptScope(null, null, null),
    target,
    fields,
    answer,
    ApiReviewPromptShape(ReviewAnswerShape.Options, listOf("same_as_workdays", "declared"), null, null),
    null,
    null,
)

internal fun freeDayDraft(prompt: ApiReviewPrompt = freeDayPrompt()): MemberWizardDraft {
    val base = sleepDraft()
    return base.copy(
        state =
            base.state.copy(
                step = MemberWizardStep.fineTune,
                savedProfile =
                    JsonObject(
                        mapOf(
                            "freeDayStatus" to JsonPrimitive("declared"),
                            "freeDayBed" to JsonPrimitive("00:30"),
                            "freeDayWake" to JsonPrimitive("09:00"),
                        ),
                    ),
                review =
                    ApiTripReview(
                        ApiReviewIdentity("capture", 1, 1, "capture", null),
                        PlanStatus.Ok,
                        emptyList(),
                        listOf(prompt),
                        null,
                        null,
                        false,
                        null,
                        ApiReviewItinerary(emptyList(), emptyList()),
                    ),
                promptSheetID = prompt.id,
            ),
    )
}

internal fun answerState(draft: MemberWizardDraft = freeDayDraft()): WizardAnswerSheetState =
    WizardAnswerSheetState.load(
        draft.state.review!!
            .prompts
            .single(),
        draft.form.reviewInput(identity = draft.form.identity, profile = draft.state.profile),
        draft.state.savedProfile,
        draft.fieldErrors,
        draft.form,
    )

class WizardAnswerSheetStateTest {
    @Test fun loadPrefersReviewAnswersThenSavedProfile() {
        val draft = freeDayDraft()
        val loaded = answerState(draft)
        assertEquals(JsonPrimitive("declared"), loaded.values["free_day_status"])
        assertEquals(JsonPrimitive("00:30"), loaded.values["free_day_bed"])
        assertEquals(JsonPrimitive("09:00"), loaded.values["free_day_wake"])
        val profile = JsonObject(draft.state.profile + ("free_day_bed" to JsonPrimitive("01:00")))
        val override = answerState(draft.copy(state = draft.state.copy(profile = profile)))
        assertEquals(JsonPrimitive("01:00"), override.values["free_day_bed"])
        val promptAnswer = freeDayPrompt(answer = mapOf("free_day_bed" to JsonPrimitive("02:00")))
        assertEquals(JsonPrimitive("02:00"), answerState(freeDayDraft(promptAnswer)).values["free_day_bed"])
    }

    @Test fun declaredWithoutTimesDisablesDone() {
        val state = answerState().copy(values = mapOf("free_day_status" to JsonPrimitive("declared")))
        assertTrue(state.missingTimes)
        assertFalse(state.doneEnabled)
    }

    @Test fun sameAsWorkdaysEnablesDone() {
        val state = answerState().copy(values = mapOf("free_day_status" to JsonPrimitive("same_as_workdays")))
        assertFalse(state.missingTimes)
        assertTrue(state.doneEnabled)
    }

    @Test fun updateClearsTheError() {
        val state = answerState().copy(error = FieldMessage("error"))
        assertEquals(null, state.update("free_day_status", JsonPrimitive("same_as_workdays")).error)
    }

    @Test fun validateMirrorsWizardOptionalAnswer() {
        val state = answerState().update("free_day_bed", JsonPrimitive("25:00"))
        assertEquals("Check your bedtime and wake-up time.", state.validate())
        assertEquals(WizardOptionalAnswer.error(state.prompt, state.values), state.validate())
    }

    @Test fun loadKeepsServerErrorArguments() {
        val error = FieldMessage("Check :field.", mapOf("field" to JsonPrimitive("bed")))
        val state = answerState(freeDayDraft().copy(fieldErrors = mapOf("profile.free_day_bed" to error)))
        assertEquals(error, state.error)
    }
}
