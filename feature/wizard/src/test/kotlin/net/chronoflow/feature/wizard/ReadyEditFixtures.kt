package net.chronoflow.feature.wizard

import net.chronoflow.core.model.contracts.ReviewCreditState
import net.chronoflow.core.wizard.generation.TripEditContext
import net.chronoflow.core.wizard.model.WizardEnd
import net.chronoflow.core.wizard.model.hasChanges
import net.chronoflow.core.wizard.model.inputFingerprint

internal fun editReadyActive(board: String = "Edit"): TripWizardUiState.Active {
    val active = readyActive()
    val draft = active.wizard.draft
    val adding = board in setOf("EditReturn", "EditReturnNoCredits")
    val returning = adding || board == "EditChanged"
    var form = if (returning) draft.form.addReturn() else draft.form
    if (returning) {
        form =
            form.updateLeg(form.returnBoundary) {
                it.copy(toIata = "AMS", departLocal = "2026-10-22T10:25", arriveLocal = "2026-10-22T15:30")
            }
    }
    var baseline =
        when {
            adding -> form.removeReturn().form
            board == "EditNoReturn" -> form.addReturn()
            else -> form
        }
    if (board == "EditChanged") {
        baseline =
            baseline.updateLeg(baseline.returnBoundary) {
                it.copy(departLocal = "2026-10-21T10:25", arriveLocal = "2026-10-21T15:30")
            }
    }
    if (board == "EditWeb") {
        form = form.copy(end = WizardEnd(kind = "nights", nights = 9))
        baseline = form.updateLeg(0) { it.copy(seatWindow = "right") }
    }
    val review = checkNotNull(draft.state.review)
    val edited =
        draft.copy(
            form = form,
            state =
                draft.state.copy(
                    edit = TripEditContext("t1", "source"),
                    originalForm = baseline,
                    originalInput = draft.copy(form = baseline).inputFingerprint,
                    review =
                        review.copy(
                            price =
                                checkNotNull(review.price).copy(
                                    state = if (returning) ReviewCreditState.Edit else ReviewCreditState.Paid,
                                    cost = if (returning) 1 else 0,
                                    balance = if (board == "EditReturnNoCredits") 0 else 3,
                                ),
                            canSave = board != "EditReturnNoCredits",
                            saveBlockedReason = if (board == "EditReturnNoCredits") "Not enough credits" else null,
                        ),
                ),
        )
    return active.copy(wizard = active.wizard.copy(draft = edited), isEditing = true, hasChanges = edited.hasChanges)
}
