package net.chronoflow.feature.wizard.answers

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.api.ApiTripReviewInput
import net.chronoflow.core.model.contracts.ApiReviewPrompt
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

internal val SUPPORTED_SHEET_KINDS = setOf(PromptKind.FreeDayTimes)

@Immutable
data class WizardAnswerSheetState(
    val prompt: ApiReviewPrompt,
    val values: Map<String, JsonElement>,
    val error: FieldMessage? = null,
    val failures: Int = 0,
) {
    val missingTimes: Boolean get() =
        prompt.kind == PromptKind.FreeDayTimes &&
            values["free_day_status"] == JsonPrimitive("declared") &&
            (
                !WizardDateTime.isTime(values["free_day_bed"].clockString()) ||
                    !WizardDateTime.isTime(values["free_day_wake"].clockString())
            )
    val doneEnabled: Boolean get() = error == null && !missingTimes

    fun update(
        field: String,
        value: JsonElement,
    ): WizardAnswerSheetState = copy(values = values + (field to value), error = null)

    fun validate(): String? = WizardOptionalAnswer.error(prompt, values)

    companion object {
        fun load(
            prompt: ApiReviewPrompt,
            reviewInput: ApiTripReviewInput,
            savedProfile: JsonObject?,
            fieldErrors: Map<String, FieldMessage>,
            form: WizardForm = WizardForm.from(reviewInput),
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
            return WizardAnswerSheetState(
                prompt,
                values,
                WizardPresentation.promptFieldError(prompt, form, WizardJourney.errors(fieldErrors, form)),
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
