package net.chronoflow.feature.wizard.debug

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
import net.chronoflow.core.wizard.model.MemberWizardDrafts
import net.chronoflow.core.wizard.model.MemberWizardStep

internal object WizardSleepQuestionBoardSeed {
    val names =
        listOf(
            "WizardSleep",
            "WizardSleepMember",
            "WizardSleepError",
            "PickerTime",
            "WizardSheetFreeSleep",
            "WizardSheetFreeSleepDarkNL",
            "WizardQAge",
            "WizardQAgeDarkNL",
            "WizardQAgeMember",
            "WizardQShift",
            "WizardQShiftNL",
            "WizardQShiftLarge",
            "WizardExcluded",
            "WizardExcludedDarkNL",
            "WizardExcludedShift",
            "WizardExcludedShiftDarkNL",
        )

    private fun prompt(
        field: String,
        age: Boolean,
    ) = ApiReviewPrompt(
        id = field,
        kind = if (age) PromptKind.AgeBand else PromptKind.Screening,
        blocking = true,
        required = true,
        text =
            CopyLine(
                if (age) {
                    "Which age group are you in?"
                } else {
                    "In the past 14 days, have you worked night or rotating shifts, or had a very irregular sleep schedule?"
                },
                emptyList(),
            ),
        scope = ApiReviewPromptScope(null, null, null),
        target = ReviewPromptTarget.Profile,
        fields = listOf(field),
        answer = emptyMap(),
        shape =
            ApiReviewPromptShape(
                if (age) ReviewAnswerShape.Options else ReviewAnswerShape.Boolean,
                if (age) listOf("under_18", "18_64", "65_plus") else emptyList(),
                null,
                null,
            ),
        side = null,
        deviceMin = null,
    )

    fun apply(
        base: MemberWizardDraft,
        board: String,
    ): MemberWizardDraft {
        val guest = board in setOf("WizardSleep", "PickerTime", "WizardQAge", "WizardQShift", "WizardExcluded")
        var state =
            if (guest) {
                MemberWizardDrafts.guest().state.copy(
                    entered = true,
                    airports = base.state.airports,
                    profile = base.state.profile,
                )
            } else {
                base.state
            }
        var errors = base.fieldErrors
        val age = prompt("age_band", true)
        val shift = prompt("shift_work", false)
        when (board) {
            "WizardSleep", "WizardSleepMember", "WizardSleepError", "PickerTime" -> {
                state =
                    state.copy(
                        step = MemberWizardStep.sleep,
                        sleepNeeded = true,
                        lastInputStep = if (guest) MemberWizardStep.sleep else MemberWizardStep.returnChoice,
                    )
                if (board == "WizardSleepError") {
                    state =
                        state.copy(
                            profile =
                                JsonObject(
                                    state.profile +
                                        mapOf(
                                            "habitual_bed" to JsonPrimitive("23:00"),
                                            "habitual_wake" to JsonPrimitive("01:00"),
                                        ),
                                ),
                        )
                    errors = mapOf("profile.habitual_bed" to FieldMessage("Check your bedtime and wake-up time."))
                }
            }

            "WizardQAge", "WizardQAgeMember" -> {
                state =
                    state.copy(step = MemberWizardStep.question, questions = listOf(age), questionIndex = 0)
            }

            "WizardQShift" -> {
                state =
                    state.copy(
                        step = MemberWizardStep.question,
                        questions = listOf(age, shift),
                        questionIndex = 1,
                        profile =
                            JsonObject(state.profile + ("age_band" to JsonPrimitive("18_64"))),
                    )
            }

            "WizardExcluded", "WizardExcludedShift" -> {
                val saved = board == "WizardExcludedShift"
                state =
                    state.copy(
                        step = MemberWizardStep.excluded,
                        profile =
                            if (saved) {
                                state.profile
                            } else {
                                JsonObject(state.profile + ("age_band" to JsonPrimitive("under_18")))
                            },
                        savedProfile = if (saved) JsonObject(mapOf("shiftWork" to JsonPrimitive(true))) else null,
                        review =
                            review(
                                PlanStatus.NoPlan,
                                listOf(
                                    CopyLine(
                                        if (saved) {
                                            "This plan is not available if, in the past 14 days, you have worked night or rotating shifts or had a very irregular sleep schedule."
                                        } else {
                                            "This plan is not available for travellers under 18."
                                        },
                                        emptyList(),
                                    ),
                                ),
                            ),
                    )
            }

            "WizardSheetFreeSleep" -> {
                val values =
                    mapOf(
                        "free_day_status" to JsonPrimitive("declared"),
                        "free_day_bed" to JsonPrimitive("00:30"),
                        "free_day_wake" to JsonPrimitive("09:00"),
                    )
                val free =
                    ApiReviewPrompt(
                        id = "free_day_times::",
                        kind = PromptKind.FreeDayTimes,
                        blocking = false,
                        required = false,
                        text = CopyLine("What time do you go to bed and wake up on free days?", emptyList()),
                        scope = ApiReviewPromptScope(null, null, null),
                        target = ReviewPromptTarget.Profile,
                        fields = values.keys.toList(),
                        answer = values,
                        shape =
                            ApiReviewPromptShape(
                                ReviewAnswerShape.Options,
                                listOf("same_as_workdays", "declared"),
                                null,
                                null,
                            ),
                        side = null,
                        deviceMin = null,
                    )
                state =
                    state.copy(
                        step = MemberWizardStep.fineTune,
                        promptSheetID = free.id,
                        savedProfile =
                            JsonObject(
                                mapOf(
                                    "freeDayStatus" to JsonPrimitive("declared"),
                                    "freeDayBed" to JsonPrimitive("00:30"),
                                    "freeDayWake" to JsonPrimitive("09:00"),
                                ),
                            ),
                        review = review(PlanStatus.Ok, prompts = listOf(free)),
                    )
            }
        }
        return base.copy(state = state, fieldErrors = errors)
    }

    private fun review(
        status: PlanStatus,
        reasons: List<CopyLine> = emptyList(),
        prompts: List<ApiReviewPrompt> = emptyList(),
    ) = ApiTripReview(
        ApiReviewIdentity("capture", 1, 1, "capture", null),
        status,
        reasons,
        prompts,
        null,
        null,
        false,
        null,
        ApiReviewItinerary(emptyList(), emptyList()),
    )
}
