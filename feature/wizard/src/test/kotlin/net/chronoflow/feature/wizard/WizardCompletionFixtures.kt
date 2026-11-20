package net.chronoflow.feature.wizard

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.contracts.ApiReviewPromptScope
import net.chronoflow.core.model.contracts.ApiReviewPromptShape
import net.chronoflow.core.model.contracts.PromptKind
import net.chronoflow.core.model.contracts.ReviewAnswerShape
import net.chronoflow.core.model.contracts.ReviewPromptTarget
import net.chronoflow.core.model.contracts.TripDirection
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.MemberWizardStep

internal fun completionPrompt(
    kind: PromptKind,
    index: Int = 0,
) = net.chronoflow.core.model.contracts.ApiReviewPrompt(
    id =
        kind.raw + ":" +
            if (kind in listOf(PromptKind.Room, PromptKind.SeatSide, PromptKind.LateCheckOut)) {
                "outbound:$index"
            } else {
                ":"
            },
    kind = kind,
    blocking = false,
    text = freeDayPrompt().text,
    required = kind in listOf(PromptKind.Room, PromptKind.LatestWake),
    scope =
        if (kind in listOf(PromptKind.Room, PromptKind.SeatSide, PromptKind.LateCheckOut)) {
            ApiReviewPromptScope(
                TripDirection.Outbound,
                if (kind == PromptKind.SeatSide) index else null,
                if (kind != PromptKind.SeatSide) index else null,
            )
        } else {
            ApiReviewPromptScope(null, null, null)
        },
    target =
        if (kind in
            listOf(
                PromptKind.FreeDayTimes,
                PromptKind.Cups,
                PromptKind.LightDevice,
                PromptKind.BlueBlockers,
            )
        ) {
            ReviewPromptTarget.Profile
        } else {
            ReviewPromptTarget.Trip
        },
    fields =
        when (kind) {
            PromptKind.Room -> {
                listOf("stay.bed_available_mode", "stay.bed_available_from_local")
            }

            PromptKind.SeatSide -> {
                listOf("seat_window")
            }

            PromptKind.LateCheckOut -> {
                listOf("stay.late_check_out_or_day_room")
            }

            PromptKind.LatestWake -> {
                listOf("latest_wake")
            }

            PromptKind.PreDepartureBusyHours -> {
                listOf(
                    "pre_departure_busy_answered",
                    "pre_departure_busy_intervals",
                )
            }

            PromptKind.Cups -> {
                listOf(
                    "caffeine_habit",
                    "cups_filter_coffee",
                    "cups_tea",
                    "cups_cola",
                    "cups_energy_drink",
                )
            }

            PromptKind.LightDevice -> {
                listOf("light_device")
            }

            PromptKind.BlueBlockers -> {
                listOf("blue_blockers", "blue_blocker_mdfd")
            }

            else -> {
                listOf("free_day_status", "free_day_bed", "free_day_wake")
            }
        },
    answer =
        when (kind) {
            PromptKind.Room -> {
                mapOf("stay.bed_available_mode" to JsonNull)
            }

            PromptKind.LateCheckOut -> {
                mapOf("stay.late_check_out_or_day_room" to JsonNull)
            }

            PromptKind.Cups -> {
                mapOf(
                    "caffeine_habit" to JsonPrimitive("cups"),
                    "cups_filter_coffee" to JsonPrimitive(2),
                )
            }

            else -> {
                emptyMap()
            }
        },
    side = null,
    deviceMin = null,
    shape =
        ApiReviewPromptShape(
            ReviewAnswerShape.Options,
            emptyList(),
            if (kind == PromptKind.Cups) 0.0 else null,
            if (kind == PromptKind.Cups) 20.0 else null,
        ),
)

internal fun fineTuneDraft(): MemberWizardDraft {
    val base = freeDayDraft()
    val kinds =
        listOf(
            PromptKind.Room,
            PromptKind.LatestWake,
            PromptKind.SeatSide,
            PromptKind.LateCheckOut,
            PromptKind.PreDepartureBusyHours,
            PromptKind.Cups,
        )
    val form =
        base.form.copy(
            legs = base.form.legs.map { it.copy(seatWindow = "left") },
            preparation = base.form.preparation.copy(latestWake = "08:30"),
        )
    return base.copy(
        form = form,
        state =
            base.state.copy(
                step = MemberWizardStep.fineTune,
                promptSheetID = null,
                sleepNeeded = false,
                review = base.state.review!!.copy(prompts = kinds.map { completionPrompt(it) }),
            ),
    )
}
