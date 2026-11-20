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

    @Test fun optionsMatchIOS() {
        fun options(kind: PromptKind) =
            net.chronoflow.feature.wizard.answers
                .WizardAnswerSheetState(
                    completionPrompt(kind),
                    emptyMap(),
                ).options
        assertEquals(
            listOf(
                JsonPrimitive("left"),
                JsonPrimitive("right"),
                kotlinx.serialization.json.JsonNull,
            ),
            options(PromptKind.SeatSide).map {
                it.value
            },
        )
        assertEquals(
            listOf("Window left", "Window right", "Aisle or not sure"),
            options(PromptKind.SeatSide).map {
                it.labelKey
            },
        )
        assertEquals("We plan without the window", options(PromptKind.SeatSide)[2].hintKey)
        assertEquals(listOf("When I land", "From a time"), options(PromptKind.Room).map { it.labelKey })
        assertEquals(
            listOf(JsonPrimitive("arrival"), JsonPrimitive("declared")),
            options(PromptKind.Room).map { it.value },
        )
        assertEquals(listOf("Yes", "No"), options(PromptKind.LateCheckOut).map { it.labelKey })
        assertEquals(
            listOf(JsonPrimitive(true), JsonPrimitive(false)),
            options(PromptKind.LateCheckOut).map { it.value },
        )
        assertEquals(
            listOf("Same as work days", "Different times"),
            options(PromptKind.FreeDayTimes).map { it.labelKey },
        )
        assertEquals(
            listOf("same_as_workdays", "declared"),
            options(PromptKind.FreeDayTimes).map {
                (it.value as JsonPrimitive).content
            },
        )
        assertEquals(listOf("None", "Light box", "Light glasses"), options(PromptKind.LightDevice).map { it.labelKey })
        assertEquals(
            listOf("none", "box", "glasses"),
            options(PromptKind.LightDevice).map {
                (it.value as JsonPrimitive).content
            },
        )
        assertEquals(
            listOf("No", "Yes, with a rating", "Yes, no rating"),
            options(PromptKind.BlueBlockers).map { it.labelKey },
        )
        assertEquals(
            listOf("no", "rated", "unrated"),
            options(PromptKind.BlueBlockers).map {
                (it.value as JsonPrimitive).content
            },
        )
        assertEquals(listOf("None"), options(PromptKind.Cups).map { it.labelKey })
        assertEquals(listOf(JsonPrimitive("non_user")), options(PromptKind.Cups).map { it.value })
        assertTrue(options(PromptKind.LatestWake).isEmpty())
        assertTrue(options(PromptKind.PreDepartureBusyHours).isEmpty())
    }

    @Test fun unansweredSeatSelectsAisle() {
        val state = WizardAnswerSheetState(completionPrompt(PromptKind.SeatSide), emptyMap())
        assertTrue(state.isSelected(state.options[2]))
        assertFalse(state.isSelected(state.options[0]))
    }

    @Test fun profileFieldsFallBackToSavedProfile() {
        val state = loadKind(PromptKind.LightDevice, saved = mapOf("lightDevice" to JsonPrimitive("box")))
        assertEquals(JsonPrimitive("box"), state.values["light_device"])
    }

    @Test fun ratingNumberLoadsWithLanguageSeparator() {
        val saved = mapOf("blueBlockerMdfd" to JsonPrimitive(1.5))
        assertEquals(JsonPrimitive("1.5"), loadKind(PromptKind.BlueBlockers, saved = saved).values["blue_blocker_mdfd"])
        assertEquals(
            JsonPrimitive("1,5"),
            loadKind(
                PromptKind.BlueBlockers,
                saved = saved,
                language = net.chronoflow.core.model.contracts.Language.Nl,
            ).values["blue_blocker_mdfd"],
        )
        assertEquals(
            JsonPrimitive("12"),
            loadKind(
                PromptKind.BlueBlockers,
                saved =
                    mapOf("blueBlockerMdfd" to JsonPrimitive(12)),
            ).values["blue_blocker_mdfd"],
        )
    }

    @Test fun noneResetsAllDrinks() {
        val state =
            WizardAnswerSheetState(
                completionPrompt(PromptKind.Cups),
                WizardOptionalAnswer.drinkFields.associateWith {
                    JsonPrimitive(3)
                },
            ).update("caffeine_habit", JsonPrimitive("non_user"))
        assertEquals(listOf(0, 0, 0, 0), WizardOptionalAnswer.drinkFields.map(state::count))
        assertEquals(JsonPrimitive("non_user"), state.values["caffeine_habit"])
    }

    @Test fun stepSetsCupsHabit() {
        val state =
            WizardAnswerSheetState(completionPrompt(PromptKind.Cups), emptyMap())
                .update("cups_tea", JsonPrimitive(50))
        assertEquals(20, state.count("cups_tea"))
        assertEquals(JsonPrimitive("cups"), state.values["caffeine_habit"])
        assertEquals(0, state.update("cups_tea", JsonPrimitive(-1)).count("cups_tea"))
    }

    @Test fun doneRejectsNoDrinks() {
        val state = WizardAnswerSheetState(completionPrompt(PromptKind.Cups), emptyMap()).validateForDone()
        assertEquals("Add at least one drink to save this answer.", state.error?.key)
        assertEquals(1, state.failures)
        assertFalse(state.doneEnabled)
    }

    @Test fun anyUpdateClearsTheError() {
        val state =
            WizardAnswerSheetState(completionPrompt(PromptKind.Cups), emptyMap())
                .validateForDone()
                .update("cups_tea", JsonPrimitive(1))
        assertEquals(null, state.error)
        assertTrue(state.doneEnabled)
    }

    @Test fun doneRejectsRatingOutOfRange() = assertInvalidRating("12")

    @Test fun doneRejectsZeroRating() = assertInvalidRating("0")

    @Test fun doneRejectsEmptyRating() = assertInvalidRating("")

    @Test fun doneRejectsNonNumericRating() = assertInvalidRating("abc")

    @Test fun doneRejectsCommaRatingAboveRange() = assertInvalidRating("9,95")

    private fun assertInvalidRating(value: String) {
        val state = rating(value).validateForDone()
        assertEquals("Enter a rating between 0.1 and 9.9.", state.error?.key)
        assertEquals(1, state.failures)
    }

    @Test fun commaRatingSubmitsWithADot() {
        val state = rating("1,5")
        assertEquals(null, state.validate())
        assertEquals(JsonPrimitive("1.5"), state.submittedValues()["blue_blocker_mdfd"])
    }

    @Test fun minimumRatingIsAccepted() = assertEquals(null, rating("0.1").validate())

    @Test fun maximumRatingIsAccepted() = assertEquals(null, rating("9.9").validate())

    @Test fun roomDeclaredNeedsATime() {
        val state =
            WizardAnswerSheetState(
                completionPrompt(PromptKind.Room),
                mapOf("stay.bed_available_mode" to JsonPrimitive("declared")),
            )
        assertTrue(state.missingTimes)
        val updated = state.setRoomTime("15:00", "2026-10-13T09:15")
        assertFalse(updated.missingTimes)
        assertEquals(JsonPrimitive("2026-10-13T15:00"), updated.values["stay.bed_available_from_local"])
        assertEquals(state, state.setRoomTime("15:00", null))
    }

    @Test fun latestWakeNeedsHHmm() {
        val state = WizardAnswerSheetState(completionPrompt(PromptKind.LatestWake), emptyMap())
        assertTrue(state.missingTimes)
        assertTrue(state.update("latest_wake", JsonPrimitive("8:30")).missingTimes)
        assertFalse(state.update("latest_wake", JsonPrimitive("08:30")).missingTimes)
    }

    @Test fun busyPutAndRemoveMarkAnswered() {
        val state = WizardAnswerSheetState(completionPrompt(PromptKind.PreDepartureBusyHours), emptyMap())
        val added = state.putBusy(null, "2026-10-09T09:00", "2026-10-09T17:30")
        assertEquals(1, added.intervals.size)
        assertEquals(JsonPrimitive(true), added.values["pre_departure_busy_answered"])
        val replaced = added.putBusy(0, "2026-10-12T08:00", "2026-10-12T11:00")
        assertEquals(1, replaced.intervals.size)
        assertEquals(JsonPrimitive("2026-10-12T08:00"), (replaced.intervals[0] as JsonObject)["start_local"])
        val removed = replaced.removeBusy(0)
        assertTrue(removed.intervals.isEmpty())
        assertEquals(JsonPrimitive(true), removed.values["pre_departure_busy_answered"])
    }

    @Test fun fieldErrorPreloads() {
        val state =
            loadKind(
                PromptKind.Room,
                errors =
                    mapOf("stays.0.bedAvailableFromLocal" to FieldMessage("Please fill this in.")),
            )
        assertEquals("Please fill this in.", state.error?.key)
    }

    @Test fun initialErrorPreloads() {
        assertEquals("seed", loadKind(PromptKind.BlueBlockers, initial = "seed").error?.key)
    }

    private fun rating(text: String) =
        WizardAnswerSheetState(
            completionPrompt(PromptKind.BlueBlockers),
            mapOf(
                "blue_blockers" to JsonPrimitive("rated"),
                "blue_blocker_mdfd" to JsonPrimitive(text),
            ),
        )

    private fun loadKind(
        kind: PromptKind,
        saved: Map<String, kotlinx.serialization.json.JsonElement> = emptyMap(),
        language: net.chronoflow.core.model.contracts.Language = net.chronoflow.core.model.contracts.Language.En,
        errors: Map<String, FieldMessage> = emptyMap(),
        initial: String? = null,
    ): WizardAnswerSheetState {
        val draft = fineTuneDraft()
        return WizardAnswerSheetState.load(
            completionPrompt(kind),
            draft.form.reviewInput(null),
            JsonObject(saved),
            errors,
            draft.form,
            language,
            initial,
        )
    }
}
