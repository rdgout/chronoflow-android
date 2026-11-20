package net.chronoflow.feature.wizard.debug

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.contracts.AdaptationDirection
import net.chronoflow.core.model.contracts.AdaptationGoal
import net.chronoflow.core.model.contracts.AdaptationView
import net.chronoflow.core.model.contracts.ApiReviewIdentity
import net.chronoflow.core.model.contracts.ApiReviewItinerary
import net.chronoflow.core.model.contracts.ApiReviewPrice
import net.chronoflow.core.model.contracts.ApiReviewPrompt
import net.chronoflow.core.model.contracts.ApiReviewSummary
import net.chronoflow.core.model.contracts.ApiTripReview
import net.chronoflow.core.model.contracts.CopyArg
import net.chronoflow.core.model.contracts.CopyArgKind
import net.chronoflow.core.model.contracts.CopyArgValue
import net.chronoflow.core.model.contracts.CopyArgument
import net.chronoflow.core.model.contracts.CopyLine
import net.chronoflow.core.model.contracts.PlanStatus
import net.chronoflow.core.model.contracts.PromptKind
import net.chronoflow.core.model.contracts.ReviewCreditState
import net.chronoflow.core.model.contracts.TripDirection
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.WizardBusy
import net.chronoflow.core.wizard.model.WizardCheckingProgress
import net.chronoflow.core.wizard.model.WizardSaveFailure
import net.chronoflow.core.wizard.model.WizardSleepOverride
import net.chronoflow.core.wizard.model.inputFingerprint
import net.chronoflow.core.wizard.model.reviewInput
import net.chronoflow.feature.wizard.shell.WizardUiOverride

internal object WizardCompletionBoardSeed {
    val baseNames =
        listOf(
            "WizardReady",
            "WizardReadyReturn",
            "WizardReadyUnlimited",
            "WizardReadyFree",
            "WizardReadyOneFree",
            "WizardReadyAdjusted",
            "WizardReadyChecking",
            "WizardReadyNoCredits",
            "WizardSaveOffline",
            "WizardSaveServer",
            "WizardSaveThrottle",
            "WizardCalculating",
            "WizardChecking",
            "WizardCheckingAgain",
            "WizardCheckingSlow",
            "WizardCheckOffline",
            "WizardCheckServer",
            "WizardCheckThrottle",
            "WizardFineTune",
            "WizardFineTuneMulti",
            "WizardFineTuneSleep",
            "WizardFineTuneBusy",
            "WizardFineTuneNone",
            "WizardFineTuneSheet",
            "WizardSheet",
            "WizardSheetRoom",
            "WizardSheetCheckout",
            "WizardSheetWake",
            "WizardSheetBusy",
            "WizardSheetDrinks",
            "WizardSheetDrinksError",
            "WizardSheetLight",
            "WizardSheetBlue",
            "WizardSheetBlueError",
        )
    val names =
        baseNames.flatMap { board ->
            when (board) {
                "WizardSaveThrottle" -> listOf(board)
                "WizardReady", "WizardChecking" -> listOf(board, board + "Dark", board + "NL")
                "WizardFineTune" -> listOf(board, board + "Dark", board + "NL", board + "DarkNL")
                "WizardSheet" -> listOf(board)
                else -> listOf(board, board + "DarkNL")
            }
        }
    private val sheets =
        mapOf(
            "WizardFineTuneSheet" to PromptKind.SeatSide,
            "WizardSheet" to PromptKind.SeatSide,
            "WizardSheetRoom" to PromptKind.Room,
            "WizardSheetCheckout" to PromptKind.LateCheckOut,
            "WizardSheetWake" to PromptKind.LatestWake,
            "WizardSheetBusy" to PromptKind.PreDepartureBusyHours,
            "WizardSheetDrinks" to PromptKind.Cups,
            "WizardSheetDrinksError" to PromptKind.Cups,
            "WizardSheetFreeSleep" to PromptKind.FreeDayTimes,
            "WizardSheetLight" to PromptKind.LightDevice,
            "WizardSheetBlue" to PromptKind.BlueBlockers,
            "WizardSheetBlueError" to PromptKind.BlueBlockers,
        )

    fun uiOverride(board: String): WizardUiOverride? {
        if (board !in baseNames && board != "WizardSheetFreeSleep") return null
        if (board.startsWith("WizardCheck")) {
            return WizardUiOverride(
                checking =
                    WizardCheckingProgress(
                        ticks = 2,
                        slow = board == "WizardCheckingSlow",
                        failure =
                            when (board) {
                                "WizardCheckOffline" -> DataFailure.Offline
                                "WizardCheckServer" -> DataFailure.Server(500)
                                "WizardCheckThrottle" -> DataFailure.Throttled(42.0, 429)
                                else -> null
                            },
                    ),
                retrySeconds = if (board == "WizardCheckThrottle") 42 else 0,
            )
        }
        return WizardUiOverride(
            reviewing = board == "WizardFineTuneBusy" || board == "WizardReadyChecking",
            retrySeconds = if (board == "WizardSaveThrottle") 42 else 0,
            answerError =
                when (board) {
                    "WizardSheetBlueError" -> "Enter a rating between 0.1 and 9.9."
                    "WizardSheetDrinksError" -> "Add at least one drink to save this answer."
                    else -> null
                },
        )
    }

    fun apply(
        base: MemberWizardDraft,
        board: String,
    ): MemberWizardDraft {
        if (uiOverride(board) == null) return base
        if (board.startsWith("WizardCheck")) {
            return base.copy(
                state =
                    base.state.copy(
                        step = MemberWizardStep.checking,
                        lastInputStep = MemberWizardStep.returnChoice,
                        checkingAnswers = board == "WizardCheckingAgain",
                    ),
            )
        }
        var form = base.form
        var state = base.state
        if (board == "WizardFineTuneMulti" || board == "WizardReadyReturn") {
            form =
                form
                    .insertStop(state.airports.getValue("DXB"), 0)
                    .updateLeg(0) { it.copy(arriveLocal = "2026-10-12T23:25") }
                    .updateLeg(1) { it.copy(departLocal = "2026-10-13T02:05", arriveLocal = "2026-10-13T16:35") }
        }
        if (board in setOf("WizardSheetCheckout", "WizardReadyReturn", "WizardReadyOneFree")) {
            form =
                form.addReturn().updateLeg(form.returnBoundary) {
                    it.copy(
                        toIata = "AMS",
                        toCity = "Amsterdam",
                        departLocal = "2026-10-22T10:25",
                        arriveLocal = "2026-10-22T15:30",
                    )
                }
            state = state.copy(returnRouteConfirmed = true)
        }
        if (board == "WizardFineTuneSleep") {
            form = form.copy(override = WizardSleepOverride(true, "00:30", "08:30"))
        }
        form =
            form.copy(
                preparation =
                    form.preparation.copy(
                        latestWake =
                            if (board ==
                                "WizardReadyAdjusted"
                            ) {
                                "08:00"
                            } else {
                                "08:30"
                            },
                    ),
            )
        if (board == "WizardSheetRoom" || board == "WizardFineTuneSleep") {
            form =
                form.copy(
                    stays =
                        form.stays.mapIndexed { index, stay ->
                            if (index == 0) {
                                stay.copy(bedAvailableMode = "declared", bedAvailableFromLocal = "2026-10-13T15:00")
                            } else {
                                stay
                            }
                        },
                )
        }
        if (board == "WizardSheetBusy") {
            form =
                form.copy(
                    preparation =
                        form.preparation.copy(
                            busyAnswered = true,
                            busyIntervals =
                                listOf(
                                    WizardBusy(startLocal = "2026-10-09T09:00", endLocal = "2026-10-09T17:30"),
                                    WizardBusy(startLocal = "2026-10-12T08:00", endLocal = "2026-10-12T11:00"),
                                ),
                        ),
                )
        }
        if (board in
            setOf(
                "WizardSheet",
                "WizardFineTuneSheet",
                "WizardFineTune",
                "WizardFineTuneBusy",
                "WizardFineTuneMulti",
            )
        ) {
            form = form.updateLeg(0) { it.copy(seatWindow = "left") }
        }
        val freeDayStatus = if (board == "WizardFineTuneSleep") "same_as_workdays" else "declared"
        val saved =
            JsonObject(
                mapOf(
                    "freeDayStatus" to JsonPrimitive(freeDayStatus),
                    "freeDayBed" to JsonPrimitive("00:30"),
                    "freeDayWake" to JsonPrimitive("09:00"),
                    "lightDevice" to JsonPrimitive("box"),
                    "blueBlockers" to JsonPrimitive("rated"),
                    "blueBlockerMdfd" to if (board == "WizardSheetBlueError") JsonPrimitive(12) else JsonPrimitive(1.5),
                    "caffeineHabit" to JsonPrimitive("cups"),
                    "cupsFilterCoffee" to JsonPrimitive(if (board == "WizardSheetDrinksError") 0 else 2),
                ) + if (board == "WizardSheetFreeSleep") state.savedProfile.orEmpty() else emptyMap(),
            )
        state = state.copy(savedProfile = saved)
        val input = form.reviewInput(null).input
        val profile = JsonObject(saved + state.profile)
        var prompts = WizardCompletionPrompts.make(input, profile)
        if (board in setOf("WizardFineTune", "WizardFineTuneBusy", "WizardFineTuneMulti")) {
            val checkout =
                WizardCompletionPrompts
                    .make(input.copy(`return` = input.outbound), profile)
                    .first { it.kind == PromptKind.LateCheckOut }
            prompts = prompts + checkout
            val order =
                if (board == "WizardFineTuneMulti") {
                    listOf(PromptKind.Room, PromptKind.SeatSide, PromptKind.LateCheckOut, PromptKind.LatestWake)
                } else {
                    listOf(
                        PromptKind.Room,
                        PromptKind.LatestWake,
                        PromptKind.SeatSide,
                        PromptKind.LateCheckOut,
                        PromptKind.PreDepartureBusyHours,
                        PromptKind.Cups,
                    )
                }
            prompts = order.flatMap { kind -> prompts.filter { it.kind == kind } }
            if (board == "WizardFineTuneMulti") {
                prompts =
                    prompts.map {
                        if (it.kind == PromptKind.LatestWake) optional(it) else it
                    }
            }
        }
        if (board == "WizardFineTuneSleep") {
            val kinds = setOf(PromptKind.Room, PromptKind.LatestWake, PromptKind.FreeDayTimes)
            prompts = prompts.filter { it.kind in kinds }
        }
        if (board == "WizardFineTuneNone") prompts = emptyList()
        val unlimited = board == "WizardReadyUnlimited"
        val free = board == "WizardReadyFree"
        val noCredits = board == "WizardReadyNoCredits"
        val adjusted = board == "WizardReadyAdjusted"
        val identity = ApiReviewIdentity("capture", 1, 1, null, null)
        form = form.copy(identity = identity)
        state =
            state.copy(
                review =
                    ApiTripReview(
                        identity,
                        if (adjusted) PlanStatus.Modified else PlanStatus.Ok,
                        if (adjusted) {
                            listOf(
                                CopyLine(
                                    "You gave no latest wake-up time, so the plan wakes you at most an hour later than usual.",
                                    emptyList(),
                                ),
                            )
                        } else {
                            emptyList()
                        },
                        prompts,
                        ApiReviewSummary(
                            CopyLine(
                                "We shift your clock :shift earlier, starting :count day before you leave.|" +
                                    "We shift your clock :shift earlier, starting :count days before you leave.",
                                listOf(
                                    numericArg("shift", CopyArgKind.Minutes, 420),
                                    numericArg("count", CopyArgKind.Count, 3),
                                ),
                            ),
                            AdaptationView(AdaptationGoal.Full, AdaptationDirection.Earlier, 420),
                            3,
                            "2026-10-09T05:15:00Z",
                            "Europe/Amsterdam",
                            "2026-10-18",
                            emptyList(),
                            null,
                        ),
                        ApiReviewPrice(
                            if (unlimited) {
                                ReviewCreditState.Unlimited
                            } else if (free) {
                                ReviewCreditState.Free
                            } else {
                                ReviewCreditState.Create
                            },
                            if (unlimited || free) {
                                0
                            } else if (board == "WizardReadyReturn") {
                                2
                            } else {
                                1
                            },
                            if (noCredits) 0 else 3,
                            unlimited,
                            if (board == "WizardReadyOneFree") listOf(TripDirection.Return) else emptyList(),
                        ),
                        !noCredits,
                        if (noCredits) "Not enough credits" else null,
                        ApiReviewItinerary(emptyList(), emptyList()),
                    ),
                reviewedAt = WizardBoardCaptureSeed.now,
                lastInputStep = MemberWizardStep.returnChoice,
                step =
                    when {
                        board == "WizardCalculating" -> MemberWizardStep.calculating
                        board.startsWith("WizardReady") || board.startsWith("WizardSave") -> MemberWizardStep.ready
                        else -> MemberWizardStep.fineTune
                    },
                saveFailure =
                    when (board) {
                        "WizardSaveOffline" -> {
                            WizardSaveFailure(WizardSaveFailure.Kind.offline)
                        }

                        "WizardSaveServer" -> {
                            WizardSaveFailure(WizardSaveFailure.Kind.server)
                        }

                        "WizardSaveThrottle" -> {
                            WizardSaveFailure(
                                WizardSaveFailure.Kind.throttle,
                                WizardBoardCaptureSeed.now.plusSeconds(42),
                            )
                        }

                        else -> {
                            null
                        }
                    },
                promptSheetID = sheets[board]?.let { kind -> prompts.firstOrNull { it.kind == kind }?.id },
                progressFloor = if (board == "WizardFineTuneNone") 0.95 else null,
            )
        val draft = base.copy(form = form, state = state)
        return draft.copy(state = state.copy(checkedInput = draft.inputFingerprint))
    }

    private fun numericArg(
        name: String,
        kind: CopyArgKind,
        value: Int,
    ): CopyArgument = CopyArgument.Standard(CopyArg(name, kind, CopyArgValue.Numeric(value.toBigDecimal()), null))

    private fun optional(prompt: ApiReviewPrompt) =
        ApiReviewPrompt(
            prompt.id,
            prompt.kind,
            prompt.blocking,
            false,
            prompt.text,
            prompt.scope,
            prompt.target,
            prompt.fields,
            prompt.answer,
            prompt.shape,
            prompt.side,
            prompt.deviceMin,
        )
}
