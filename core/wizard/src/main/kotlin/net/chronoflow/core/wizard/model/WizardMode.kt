@file:Suppress("ktlint:standard:enum-entry-name-case")

package net.chronoflow.core.wizard.model

import kotlinx.serialization.Serializable
import net.chronoflow.core.model.contracts.ApiReviewPrompt
import net.chronoflow.core.model.contracts.ApiReviewPromptScope
import net.chronoflow.core.model.contracts.ApiReviewPromptShape
import net.chronoflow.core.model.contracts.CopyLine
import net.chronoflow.core.model.contracts.PromptKind
import net.chronoflow.core.model.contracts.ReviewAnswerShape
import net.chronoflow.core.model.contracts.ReviewPromptTarget

@Serializable
enum class WizardMode { member, guest }

internal fun MemberWizardDraft.returnToReadyAfterExpiredClaim(): MemberWizardDraft =
    copy(
        state =
            state.copy(
                returnToReady = true,
                checkingAnswers = null,
                reviewedAt = null,
                step = MemberWizardStep.ready,
            ),
    )

internal fun MemberWizardDraft.enforceGuestMode(): MemberWizardDraft {
    if (state.mode != WizardMode.guest) return this
    val form = if (form.end.kind == "return_legs") form.removeReturn().form else form
    return copy(
        form = form.copy(end = WizardEnd(), override = WizardSleepOverride()),
        state =
            state.copy(
                sleepNeeded = true,
                lastInputStep = MemberWizardStep.sleep,
                savedProfile = null,
                firstTrip = false,
            ),
    )
}

val MemberWizardModel.guestAnsweredQuestionCount: Int get() =
    listOf("age_band", "shift_work").count {
        state.profile[it] != null && state.profile[it] != kotlinx.serialization.json.JsonNull
    }

fun MemberWizardModel.revisitGuestQuestions() {
    if (mode != WizardMode.guest || state.step != MemberWizardStep.ready || reviewing || closed) return
    state = state.copy(questions = guestScreeningQuestions, questionIndex = 0)
    revisit(MemberWizardStep.question)
}

internal val MemberWizardModel.guestScreeningQuestions: List<ApiReviewPrompt> get() =
    listOf(
        ApiReviewPrompt(
            id = "age_band",
            kind = PromptKind.AgeBand,
            blocking = true,
            required = true,
            text = CopyLine("Which age group are you in?", emptyList()),
            scope = ApiReviewPromptScope(null, null, null),
            target = ReviewPromptTarget.Profile,
            fields = listOf("age_band"),
            answer = emptyMap(),
            shape = ApiReviewPromptShape(ReviewAnswerShape.Options, listOf("under_18", "18_64", "65_plus"), null, null),
            side = null,
            deviceMin = null,
        ),
        ApiReviewPrompt(
            id = "shift_work",
            kind = PromptKind.Screening,
            blocking = true,
            required = true,
            text =
                CopyLine(
                    "In the past 14 days, have you worked night or rotating shifts, or had a very irregular sleep schedule?",
                    emptyList(),
                ),
            scope = ApiReviewPromptScope(null, null, null),
            target = ReviewPromptTarget.Profile,
            fields = listOf("shift_work"),
            answer = emptyMap(),
            shape = ApiReviewPromptShape(ReviewAnswerShape.Boolean, emptyList(), null, null),
            side = null,
            deviceMin = null,
        ),
    )
val MemberWizardModel.mode: WizardMode get() = state.mode
val MemberWizardModel.showsReturnRow: Boolean get() = mode == WizardMode.member
val MemberWizardModel.allowsSleepOverride: Boolean get() = mode == WizardMode.member

fun MemberWizardModel.guestReviewExpired() {
    if (mode != WizardMode.guest || closed) return
    draft = draft.returnToReadyAfterExpiredClaim()
    saveOutcome = null
    persist()
}

fun MemberWizardModel.finishOutboundStep() {
    if (mode == WizardMode.guest) finishFlightInput() else go(MemberWizardStep.returnChoice)
}

internal val MemberWizardModel.guestStepProgress: Double get() =
    when (state.step) {
        MemberWizardStep.whereTo -> 0.09
        MemberWizardStep.outbound -> 0.18
        MemberWizardStep.sleep -> 0.30
        MemberWizardStep.checking -> if (state.checkingAnswers == true) 0.64 else 0.42
        MemberWizardStep.excluded -> if (state.checkingAnswers == true) 0.64 else 0.60
        MemberWizardStep.question -> if ((state.questionIndex ?: 0) == 0) 0.54 else 0.64
        MemberWizardStep.continuation, MemberWizardStep.fineTune -> 0.86
        MemberWizardStep.ready -> 0.95
        MemberWizardStep.calculating -> 1.0
        MemberWizardStep.returnChoice, MemberWizardStep.returnRoute, MemberWizardStep.returnFlight -> 0.18
    }

internal fun MemberWizardModel.recheckGuestReady() {
    saveOutcome = null
    state = state.copy(returnToReady = true)
    beginChecking()
}
