package net.chronoflow.feature.wizard.debug

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.contracts.ApiReviewPrompt
import net.chronoflow.core.model.contracts.ApiReviewPromptScope
import net.chronoflow.core.model.contracts.ApiReviewPromptShape
import net.chronoflow.core.model.contracts.ApiTripInput
import net.chronoflow.core.model.contracts.BlueBlockers
import net.chronoflow.core.model.contracts.CopyArg
import net.chronoflow.core.model.contracts.CopyArgKind
import net.chronoflow.core.model.contracts.CopyArgValue
import net.chronoflow.core.model.contracts.CopyArgument
import net.chronoflow.core.model.contracts.CopyLine
import net.chronoflow.core.model.contracts.ProfilePreferences
import net.chronoflow.core.model.contracts.PromptKind
import net.chronoflow.core.model.contracts.ReviewAnswerShape
import net.chronoflow.core.model.contracts.ReviewPromptTarget
import net.chronoflow.core.model.contracts.TripDirection

internal object WizardCompletionPrompts {
    fun make(
        input: ApiTripInput,
        profile: JsonObject,
    ): List<ApiReviewPrompt> {
        val prompts = mutableListOf<ApiReviewPrompt>()

        fun add(
            kind: PromptKind,
            key: String,
            fields: List<String>,
            profileTarget: Boolean = false,
            required: Boolean = false,
            journey: TripDirection? = null,
            index: Int? = null,
            options: List<String> = emptyList(),
            shape: ReviewAnswerShape = ReviewAnswerShape.Options,
            args: List<CopyArgument> = emptyList(),
        ) {
            val answer =
                fields.associateWith { field ->
                    (if (profileTarget) profile[ProfilePreferences.camel(field)] as? JsonPrimitive else null)
                        ?: JsonNull
                }
            prompts +=
                ApiReviewPrompt(
                    id = listOf(kind.raw, journey?.raw ?: "", index?.toString() ?: "").joinToString(":"),
                    kind = kind,
                    blocking = false,
                    required = required,
                    text = CopyLine(key, args),
                    scope =
                        ApiReviewPromptScope(
                            journey,
                            if (kind ==
                                PromptKind.SeatSide
                            ) {
                                index
                            } else {
                                null
                            },
                            if (kind == PromptKind.Room ||
                                kind == PromptKind.LateCheckOut
                            ) {
                                index
                            } else {
                                null
                            },
                        ),
                    target = if (profileTarget) ReviewPromptTarget.Profile else ReviewPromptTarget.Trip,
                    fields = fields,
                    answer = answer,
                    shape =
                        ApiReviewPromptShape(
                            shape,
                            options,
                            if (kind ==
                                PromptKind.Cups
                            ) {
                                0.0
                            } else {
                                null
                            },
                            if (kind ==
                                PromptKind.Cups
                            ) {
                                20.0
                            } else {
                                null
                            },
                        ),
                    side = null,
                    deviceMin = null,
                )
        }
        for ((direction, journey) in listOf(
            TripDirection.Outbound to input.outbound,
            TripDirection.Return to input.`return`,
        )) {
            if (journey == null) continue
            for ((index, leg) in journey.legs.withIndex()) {
                val args =
                    listOf(
                        CopyArgument.Standard(CopyArg("place", CopyArgKind.Place, CopyArgValue.Text(leg.toIata), null)),
                    )
                add(
                    PromptKind.Room,
                    "Will you have a room in :place when you land, and from what time?",
                    listOf("stay.bed_available_mode", "stay.bed_available_from_local"),
                    required = true,
                    journey = direction,
                    index = index,
                    options = listOf("arrival", "declared"),
                    args = args,
                )
                add(
                    PromptKind.SeatSide,
                    "On your flight to :place, a seat on the left side may let you see more daylight.",
                    listOf("seat_window"),
                    journey = direction,
                    index = index,
                    options = listOf("left", "right"),
                    args = args,
                )
                if (direction == TripDirection.Outbound &&
                    input.`return` != null
                ) {
                    add(
                        PromptKind.LateCheckOut,
                        "Can you check out late, or book a day room, in :place?",
                        listOf("stay.late_check_out_or_day_room"),
                        journey = direction,
                        index = index,
                        shape = ReviewAnswerShape.Boolean,
                        args = args,
                    )
                }
            }
        }
        add(
            PromptKind.LatestWake,
            "What is the latest you can wake up on the days before you leave?",
            listOf("latest_wake"),
            required = true,
            shape = ReviewAnswerShape.ClockTime,
        )
        add(
            PromptKind.PreDepartureBusyHours,
            "Which hours before you leave are you busy, so the plan keeps them free of light and naps?",
            listOf("pre_departure_busy_answered", "pre_departure_busy_intervals"),
            shape = ReviewAnswerShape.Intervals,
        )
        add(
            PromptKind.FreeDayTimes,
            "What time do you go to bed and wake up on free days?",
            listOf("free_day_status", "free_day_bed", "free_day_wake"),
            profileTarget = true,
            options = listOf("same_as_workdays", "declared"),
        )
        if (profile["excludeCaffeine"] != JsonPrimitive(true)) {
            add(
                PromptKind.Cups,
                "How many caffeinated drinks do you usually have in a day?",
                listOf("caffeine_habit", "cups_filter_coffee", "cups_tea", "cups_cola", "cups_energy_drink"),
                profileTarget = true,
            )
        }
        add(
            PromptKind.LightDevice,
            "Do you have a light box or light glasses? One gives the plan more room on days when daylight is hard to get.",
            listOf("light_device"),
            profileTarget = true,
            options = listOf("none", "box", "glasses"),
        )
        add(
            PromptKind.BlueBlockers,
            "Do you have blue-blocking glasses with a rated protection? Tell us the rating if you know it.",
            listOf("blue_blockers", "blue_blocker_mdfd"),
            profileTarget = true,
            options = listOf("no", "rated", "unrated"),
        )
        return prompts
    }
}
