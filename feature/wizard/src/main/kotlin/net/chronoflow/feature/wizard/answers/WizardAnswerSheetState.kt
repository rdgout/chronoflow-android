package net.chronoflow.feature.wizard.answers

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.api.ApiTripReviewInput
import net.chronoflow.core.model.contracts.ApiReviewPrompt
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.ProfilePreferences
import net.chronoflow.core.model.contracts.PromptKind
import net.chronoflow.core.model.contracts.ReviewPromptTarget
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.wizard.model.WizardDateTime
import net.chronoflow.core.wizard.model.WizardForm
import net.chronoflow.core.wizard.model.WizardJourney
import net.chronoflow.core.wizard.model.WizardOptionalAnswer
import net.chronoflow.core.wizard.model.WizardPresentation
import net.chronoflow.core.wizard.model.from
import net.chronoflow.feature.wizard.sleep.WizardTimeSheetUi
import net.chronoflow.feature.wizard.sleep.clockString

internal val SUPPORTED_SHEET_KINDS =
    setOf(
        PromptKind.SeatSide,
        PromptKind.Room,
        PromptKind.LateCheckOut,
        PromptKind.LatestWake,
        PromptKind.PreDepartureBusyHours,
        PromptKind.FreeDayTimes,
        PromptKind.Cups,
        PromptKind.LightDevice,
        PromptKind.BlueBlockers,
    )

@Immutable
data class AnswerOption(
    val value: JsonElement,
    val labelKey: String,
    val hintKey: String? = null,
)

@Immutable
data class WizardAnswerSheetState(
    val prompt: ApiReviewPrompt,
    val values: Map<String, JsonElement>,
    val error: FieldMessage? = null,
    val failures: Int = 0,
) {
    val optionField: String get() =
        if (prompt.kind == PromptKind.Room) {
            "stay.bed_available_mode"
        } else {
            prompt.fields.firstOrNull().orEmpty()
        }

    val options: List<AnswerOption> get() =
        when (prompt.kind) {
            PromptKind.SeatSide -> {
                listOf(
                    AnswerOption(JsonPrimitive("left"), "Window left"),
                    AnswerOption(JsonPrimitive("right"), "Window right"),
                    AnswerOption(JsonNull, "Aisle or not sure", "We plan without the window"),
                )
            }

            PromptKind.Room -> {
                listOf(
                    AnswerOption(JsonPrimitive("arrival"), "When I land"),
                    AnswerOption(JsonPrimitive("declared"), "From a time"),
                )
            }

            PromptKind.LateCheckOut -> {
                listOf(
                    AnswerOption(JsonPrimitive(true), "Yes"),
                    AnswerOption(JsonPrimitive(false), "No"),
                )
            }

            PromptKind.FreeDayTimes -> {
                listOf(
                    AnswerOption(JsonPrimitive("same_as_workdays"), "Same as work days"),
                    AnswerOption(JsonPrimitive("declared"), "Different times"),
                )
            }

            PromptKind.LightDevice -> {
                listOf(
                    AnswerOption(JsonPrimitive("none"), "None"),
                    AnswerOption(JsonPrimitive("box"), "Light box"),
                    AnswerOption(JsonPrimitive("glasses"), "Light glasses"),
                )
            }

            PromptKind.BlueBlockers -> {
                listOf(
                    AnswerOption(JsonPrimitive("no"), "No"),
                    AnswerOption(JsonPrimitive("rated"), "Yes, with a rating"),
                    AnswerOption(JsonPrimitive("unrated"), "Yes, no rating"),
                )
            }

            PromptKind.Cups -> {
                listOf(AnswerOption(JsonPrimitive("non_user"), "None"))
            }

            else -> {
                emptyList()
            }
        }

    fun isSelected(option: AnswerOption): Boolean = (values[optionField] ?: JsonNull) == option.value

    val missingTimes: Boolean get() =
        when (prompt.kind) {
            PromptKind.FreeDayTimes -> {
                values["free_day_status"] == JsonPrimitive("declared") &&
                    (
                        !WizardDateTime.isTime(values["free_day_bed"].clockString()) ||
                            !WizardDateTime.isTime(values["free_day_wake"].clockString())
                    )
            }

            PromptKind.LatestWake -> {
                !WizardDateTime.isTime(values["latest_wake"].clockString())
            }

            PromptKind.Room -> {
                values[optionField] == JsonPrimitive("declared") &&
                    WizardDateTime.date(values["stay.bed_available_from_local"].clockString().orEmpty()) == null
            }

            else -> {
                false
            }
        }
    val doneEnabled: Boolean get() = error == null && !missingTimes
    val maxDrinks: Int get() = (prompt.shape.max ?: 20.0).toInt().coerceAtLeast(0)
    val intervals: List<JsonElement> get() = (values["pre_departure_busy_intervals"] as? JsonArray).orEmpty()

    fun count(field: String): Int = (values[field] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0

    fun update(
        field: String,
        value: JsonElement,
    ): WizardAnswerSheetState {
        val next =
            when {
                prompt.kind == PromptKind.Cups && field in WizardOptionalAnswer.drinkFields -> {
                    val count = (value as? JsonPrimitive)?.content?.toIntOrNull() ?: 0
                    values + ("caffeine_habit" to JsonPrimitive("cups")) +
                        (field to JsonPrimitive(count.coerceIn(0, maxDrinks)))
                }

                prompt.kind == PromptKind.Cups && field == optionField && value == JsonPrimitive("non_user") -> {
                    values + (field to value) + WizardOptionalAnswer.drinkFields.associateWith { JsonPrimitive(0) }
                }

                prompt.kind == PromptKind.PreDepartureBusyHours && field == "pre_departure_busy_intervals" -> {
                    values + (field to value) + ("pre_departure_busy_answered" to JsonPrimitive(true))
                }

                else -> {
                    values + (field to value)
                }
            }
        return copy(values = next, error = null)
    }

    fun setRoomTime(
        time: String,
        landing: String?,
    ): WizardAnswerSheetState {
        if (!WizardDateTime.isTime(time) || landing == null || WizardDateTime.date(landing) == null) return this
        return update("stay.bed_available_from_local", JsonPrimitive(landing.take(10) + "T" + time))
    }

    fun putBusy(
        index: Int?,
        start: String,
        end: String,
    ): WizardAnswerSheetState {
        val rows = intervals.toMutableList()
        val row = JsonObject(mapOf("start_local" to JsonPrimitive(start), "end_local" to JsonPrimitive(end)))
        if (index != null && index in rows.indices) rows[index] = row else rows.add(row)
        return update("pre_departure_busy_intervals", JsonArray(rows))
    }

    fun removeBusy(index: Int): WizardAnswerSheetState {
        if (index !in intervals.indices) return this
        return update("pre_departure_busy_intervals", JsonArray(intervals.filterIndexed { i, _ -> i != index }))
    }

    fun validate(): String? = WizardOptionalAnswer.error(prompt, values)

    fun validateForDone(): WizardAnswerSheetState {
        val message = validate() ?: return this
        return copy(error = FieldMessage(message), failures = failures + 1)
    }

    fun submittedValues(): Map<String, JsonElement> =
        if (
            prompt.kind == PromptKind.BlueBlockers && values["blue_blockers"] == JsonPrimitive("rated")
        ) {
            values +
                (
                    "blue_blocker_mdfd" to
                        JsonPrimitive(
                            values["blue_blocker_mdfd"].clockString().orEmpty().replace(',', '.'),
                        )
                )
        } else {
            values
        }

    companion object {
        fun load(
            prompt: ApiReviewPrompt,
            reviewInput: ApiTripReviewInput,
            savedProfile: JsonObject?,
            fieldErrors: Map<String, FieldMessage>,
            form: WizardForm = WizardForm.from(reviewInput),
            language: Language = Language.En,
            initialError: String? = null,
        ): WizardAnswerSheetState {
            val answers =
                try {
                    WizardPresentation.promptAnswers(prompt, reviewInput)
                } catch (_: DataFailure) {
                    prompt.answer
                } catch (_: SerializationException) {
                    prompt.answer
                } catch (_: IllegalArgumentException) {
                    prompt.answer
                }
            val values = answers.toMutableMap()
            if (prompt.target == ReviewPromptTarget.Profile) {
                for (field in prompt.fields) {
                    if (values[field] == null || values[field] == JsonNull) {
                        savedProfile?.get(ProfilePreferences.camel(field))?.let { values[field] = it }
                    }
                }
            }
            val rating = values["blue_blocker_mdfd"] as? JsonPrimitive
            if (rating != null && !rating.isString) {
                val number =
                    rating.content
                        .toBigDecimalOrNull()
                        ?.stripTrailingZeros()
                        ?.toPlainString()
                        .orEmpty()
                values["blue_blocker_mdfd"] =
                    JsonPrimitive(if (language == Language.Nl) number.replace('.', ',') else number)
            }
            return WizardAnswerSheetState(
                prompt,
                values,
                WizardPresentation.promptFieldError(prompt, form, WizardJourney.errors(fieldErrors, form))
                    ?: initialError?.let(::FieldMessage),
            )
        }
    }
}

@Immutable
data class WizardAnswerSheetUi(
    val state: WizardAnswerSheetState,
    val timeFormat: TimeFormat,
    val openTime: WizardTimeSheetUi? = null,
) {
    val declared: Boolean get() = state.values["free_day_status"] == JsonPrimitive("declared")
}
