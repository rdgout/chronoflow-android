package net.chronoflow.feature.wizard.debug

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.WizardEnd
import net.chronoflow.core.wizard.model.WizardMode
import net.chronoflow.core.wizard.model.WizardSleepOverride
import net.chronoflow.core.wizard.model.inputFingerprint
import net.chronoflow.core.wizard.model.reviewInput
import net.chronoflow.feature.wizard.shell.WizardUiOverride
import net.chronoflow.feature.wizard.ui.WizardInitialPresentation
import java.time.Instant
import java.util.Locale

object GuestWizardBoardSeed {
    val names =
        listOf(
            "WizardWhereGuest",
            "WizardOutboundGuest",
            "WizardOutboundPastGuest",
            "WizardOutboundPastGuestDarkNL",
            "WizardCheckingGuest",
            "WizardFineTuneGuest",
            "WizardReadyGuest",
            "WizardReadyGuestDarkNL",
            "WizardReadyGuestFree",
            "WizardReadyGuestFreeDarkNL",
            "WizardDiscardGuest",
            "WizardDiscardGuestDarkNL",
        )

    private fun shared(board: String): String =
        when (WizardBoardCaptureSeed.baseName(board)) {
            "WizardGuestJourney" -> "WizardSaveJourney"
            "WizardWhereGuest" -> "WizardWhere"
            "WizardWhereGuestEmpty" -> "WizardWhereEmpty"
            "WizardOutboundGuest", "WizardDiscardGuest" -> "WizardOutbound"
            "WizardOutboundPastGuest" -> "WizardOutboundPast"
            "WizardCheckingGuest" -> "WizardChecking"
            "WizardFineTuneGuest", "WizardReadyGuest" -> "WizardReady"
            "WizardReadyGuestFree" -> "WizardReadyFree"
            else -> if (board.startsWith("GuestSignup")) "WizardReady" else WizardBoardCaptureSeed.baseName(board)
        }

    fun presentation(board: String): WizardInitialPresentation =
        WizardBoardCaptureSeed.presentation(shared(board)).copy(
            discardDialogVisible = WizardBoardCaptureSeed.baseName(board) == "WizardDiscardGuest",
        )

    fun uiOverride(board: String): WizardUiOverride? =
        if (board ==
            "WizardGuestJourney"
        ) {
            null
        } else {
            WizardBoardCaptureSeed.uiOverride(shared(board))
        }

    fun draft(
        original: MemberWizardDraft,
        board: String,
        locale: Locale,
        now: Instant = WizardBoardCaptureSeed.now,
    ): MemberWizardDraft {
        val name = WizardBoardCaptureSeed.baseName(board)
        val base = WizardBoardCaptureSeed.draft(shared(board), locale, now)
        var state =
            base.state.copy(
                instance = original.state.instance,
                revision = original.state.revision + 1,
                mode = WizardMode.guest,
                fromLastTrip = false,
                sleepNeeded = true,
                lastInputStep = MemberWizardStep.sleep,
                firstTrip = false,
                savedProfile = null,
            )
        if (name == "WizardGuestJourney") {
            state =
                state.copy(
                    step = MemberWizardStep.sleep,
                    profile =
                        JsonObject(state.profile + ("shift_work" to JsonPrimitive(false))),
                )
        }
        if (name == "WizardFineTuneGuest") state = state.copy(step = MemberWizardStep.fineTune)
        if (state.step == MemberWizardStep.fineTune || state.step == MemberWizardStep.ready) {
            state =
                state.copy(
                    sleepCompleted = true,
                    profile =
                        JsonObject(
                            state.profile +
                                mapOf(
                                    "age_band" to JsonPrimitive("18_64"),
                                    "shift_work" to JsonPrimitive(false),
                                ),
                        ),
                )
        }
        if (name == "WizardReadyGuest" || name == "WizardReadyGuestFree") {
            val review = checkNotNull(state.review)
            val summary = checkNotNull(review.summary).copy(planEndDate = "2026-10-14")
            state = state.copy(review = review.copy(summary = summary))
        }
        val form =
            (if (base.form.end.kind == "return_legs") base.form.removeReturn().form else base.form).copy(
                end = WizardEnd(),
                override = WizardSleepOverride(),
            )
        val errors =
            if (name == "WizardOutboundPastGuest") {
                mapOf("review" to FieldMessage("A new plan needs an upcoming arrival. Check your flight dates."))
            } else {
                base.fieldErrors
            }
        var draft = base.copy(form = form, state = state, fieldErrors = errors)
        if (name == "WizardFineTuneGuest") {
            val unanswered =
                form.copy(
                    legs = form.legs.map { it.copy(seatWindow = null) },
                    stays = form.stays.map { it.copy(bedAvailableMode = null, bedAvailableFromLocal = null) },
                    preparation = form.preparation.copy(latestWake = null),
                )
            val review = checkNotNull(state.review)
            val prompts = WizardCompletionPrompts.make(unanswered.reviewInput(null).input, state.profile)
            draft =
                draft.copy(
                    form = unanswered,
                    state =
                        state.copy(
                            review =
                                review.copy(
                                    prompts = prompts,
                                ),
                        ),
                )
        }
        if (state.step == MemberWizardStep.ready || state.step == MemberWizardStep.fineTune) {
            draft = draft.copy(state = draft.state.copy(checkedInput = draft.inputFingerprint))
        }
        return draft
    }
}
