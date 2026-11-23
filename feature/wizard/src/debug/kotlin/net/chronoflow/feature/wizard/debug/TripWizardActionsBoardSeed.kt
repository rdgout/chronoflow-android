@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.feature.wizard.debug

import kotlinx.serialization.json.JsonObject
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.contracts.ApiStayEnd
import net.chronoflow.core.model.contracts.CopyLine
import net.chronoflow.core.model.contracts.PlanStatus
import net.chronoflow.core.model.contracts.ReviewCreditState
import net.chronoflow.core.wizard.generation.TripEditContext
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.WizardCheckingProgress
import net.chronoflow.core.wizard.model.WizardEnd
import net.chronoflow.core.wizard.model.WizardPresentation
import net.chronoflow.core.wizard.model.WizardSaveFailure
import net.chronoflow.core.wizard.model.clearDuplicateDates
import net.chronoflow.core.wizard.model.hasChanges
import net.chronoflow.core.wizard.model.inputFingerprint
import net.chronoflow.feature.wizard.shell.WizardUiOverride

internal object TripWizardActionsBoardSeed {
    val baseNames =
        listOf(
            "WizardEdit",
            "WizardEditChanged",
            "WizardEditNoReturn",
            "WizardEditReturn",
            "WizardEditReturnNoCredits",
            "WizardEditWeb",
            "WizardReturnWeb",
            "WizardCheckingEdit",
            "WizardCheckOfflineEdit",
            "WizardCheckServerEdit",
            "WizardExcludedEdit",
            "WizardDiscardEdit",
            "WizardDuplicate",
            "WizardEditSaveOffline",
            "WizardEditSaveServer",
        )
    val names =
        baseNames.flatMap { board ->
            if (board in setOf("WizardEditReturn", "WizardEditSaveOffline", "WizardEditSaveServer")) {
                listOf(board)
            } else {
                listOf(board, board + "DarkNL")
            }
        }

    fun uiOverride(board: String): WizardUiOverride? {
        if (board !in baseNames || board in setOf("WizardDuplicate", "WizardReturnWeb")) return null
        if (board in setOf("WizardCheckingEdit", "WizardCheckOfflineEdit", "WizardCheckServerEdit")) {
            return WizardUiOverride(
                checking =
                    WizardCheckingProgress(
                        ticks = 2,
                        slow = false,
                        failure =
                            when (board) {
                                "WizardCheckOfflineEdit" -> DataFailure.Offline
                                "WizardCheckServerEdit" -> DataFailure.Server(500)
                                else -> null
                            },
                    ),
                retrySeconds = 0,
            )
        }
        return WizardUiOverride(reviewing = false, retrySeconds = 0)
    }

    fun apply(
        original: MemberWizardDraft,
        board: String,
    ): MemberWizardDraft {
        if (board !in baseNames) return original
        if (board == "WizardReturnWeb") {
            val form = original.form.copy(end = WizardEnd(kind = "nights", nights = 9))
            val draft =
                original.copy(
                    form = form,
                    state =
                        original.state.copy(
                            edit = TripEditContext("capture-trip", "capture-source"),
                            firstTrip = false,
                            profile = JsonObject(emptyMap()),
                            originalForm = form,
                            snapshotStayEnd = stayEnd,
                            entered = false,
                            step = MemberWizardStep.returnChoice,
                            progressFloor = null,
                        ),
                )
            return draft.copy(state = draft.state.copy(originalInput = draft.inputFingerprint))
        }
        val seed = if (board in setOf("WizardEditSaveOffline", "WizardEditSaveServer")) "WizardEditReturn" else board
        val addingReturn = seed in setOf("WizardEditReturn", "WizardEditReturnNoCredits", "WizardDiscardEdit")
        val returning = addingReturn || seed == "WizardEditChanged"
        var draft = WizardCompletionBoardSeed.apply(original, "WizardReady")
        var form = draft.form
        if (returning) {
            form =
                form.addReturn().let { added ->
                    added.updateLeg(added.returnBoundary) {
                        it.copy(
                            toIata = "AMS",
                            toCity = "Amsterdam",
                            departLocal = "2026-10-22T10:25",
                            arriveLocal = "2026-10-22T15:30",
                        )
                    }
                }
        }
        if (seed == "WizardDuplicate") {
            return draft.copy(
                form = form.clearDuplicateDates(),
                state =
                    draft.state.copy(
                        copiedRoute = "Amsterdam → " + (draft.airports["NRT"]?.city ?: "Tokyo"),
                        step = MemberWizardStep.outbound,
                        review = null,
                    ),
            )
        }
        var review = checkNotNull(draft.state.review)
        review =
            review.copy(
                summary =
                    review.summary?.let { summary ->
                        summary.copy(
                            planEndDate =
                                if (returning) {
                                    "2026-10-26"
                                } else if (seed == "WizardEditWeb") {
                                    "2026-10-22"
                                } else {
                                    "2026-10-14"
                                },
                            line =
                                if (returning) {
                                    summary.line.copy(
                                        key =
                                            "We shift your clock :shift earlier, starting :count day before you leave. Your trip home is planned too.|" +
                                                "We shift your clock :shift earlier, starting :count days before you leave. Your trip home is planned too.",
                                    )
                                } else {
                                    summary.line
                                },
                        )
                    },
            )
        var baseline = form
        if (addingReturn) baseline = baseline.removeReturn().form
        if (seed == "WizardEditNoReturn") {
            baseline =
                baseline.addReturn().let { added ->
                    added.updateLeg(added.returnBoundary) { it.copy(toIata = "AMS") }
                }
        }
        if (seed == "WizardEditWeb") {
            form = form.copy(end = WizardEnd(kind = "nights", nights = 9))
            baseline = form.updateLeg(0) { it.copy(seatWindow = "right") }
        }
        if (seed == "WizardEditChanged") {
            baseline =
                baseline.updateLeg(baseline.returnBoundary) {
                    it.copy(departLocal = "2026-10-21T10:25", arriveLocal = "2026-10-21T15:30")
                }
        }
        draft =
            draft.copy(
                form = form,
                state =
                    draft.state.copy(
                        edit = TripEditContext("capture-trip", "capture-source"),
                        firstTrip = false,
                        profile = JsonObject(emptyMap()),
                        originalForm = baseline,
                        snapshotStayEnd = stayEnd.takeIf { seed == "WizardEditWeb" },
                    ),
            )
        draft = draft.copy(state = draft.state.copy(originalInput = draft.copy(form = baseline).inputFingerprint))
        val identity = review.identity.copy(sourceRevision = "capture-source")
        review =
            review.copy(
                identity = identity,
                summary =
                    review.summary?.let {
                        it.copy(
                            stayEnd =
                                if (seed ==
                                    "WizardEditWeb"
                                ) {
                                    stayEnd
                                } else {
                                    it.stayEnd
                                },
                        )
                    },
                price =
                    checkNotNull(review.price).copy(
                        state = if (returning) ReviewCreditState.Edit else ReviewCreditState.Paid,
                        cost = if (returning) 1 else 0,
                        balance = if (seed == "WizardEditReturnNoCredits") 0 else 3,
                    ),
                canSave = seed != "WizardEditReturnNoCredits",
                saveBlockedReason = if (seed == "WizardEditReturnNoCredits") "Not enough credits" else null,
            )
        form = form.copy(identity = identity)
        draft =
            draft.copy(
                form = form,
                state =
                    draft.state.copy(
                        review = review,
                        progressFloor = 1.0,
                        returnToReady = true,
                        returnRouteConfirmed = returning,
                    ),
            )
        draft = draft.copy(state = draft.state.copy(entered = draft.hasChanges))
        if (seed == "WizardEditChanged") {
            draft =
                draft.copy(
                    state =
                        draft.state.copy(
                            sourceChange =
                                WizardPresentation.SourceChange(
                                    form,
                                    listOf(WizardPresentation.SourceChange.Field.returnFlight),
                                    review.price,
                                    review.price,
                                ),
                        ),
                )
        }
        val step =
            when (seed) {
                "WizardExcludedEdit" -> MemberWizardStep.excluded
                "WizardCheckingEdit", "WizardCheckOfflineEdit", "WizardCheckServerEdit" -> MemberWizardStep.checking
                else -> MemberWizardStep.ready
            }
        if (seed == "WizardExcludedEdit") {
            review =
                review.copy(
                    status = PlanStatus.NoPlan,
                    summary = null,
                    price = null,
                    reasons =
                        listOf(
                            CopyLine(
                                "This plan is not available if, in the past 14 days, you have worked night or rotating shifts or had a very irregular sleep schedule.",
                                emptyList(),
                            ),
                        ),
                )
        }
        return draft.copy(
            state =
                draft.state.copy(
                    step = step,
                    review = review,
                    saveFailure =
                        when (board) {
                            "WizardEditSaveOffline" -> WizardSaveFailure(WizardSaveFailure.Kind.offline)
                            "WizardEditSaveServer" -> WizardSaveFailure(WizardSaveFailure.Kind.server)
                            else -> null
                        },
                ),
        )
    }

    private val stayEnd = ApiStayEnd("2026-10-21", "2026-10-21T03:00:00Z")
}
