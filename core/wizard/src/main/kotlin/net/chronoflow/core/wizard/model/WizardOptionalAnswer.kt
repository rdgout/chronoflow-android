package net.chronoflow.core.wizard.model

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.contracts.ApiReviewPrompt
import net.chronoflow.core.model.contracts.PromptKind
import java.math.BigDecimal

object WizardOptionalAnswer {
    val drinkFields = listOf("cups_filter_coffee", "cups_tea", "cups_cola", "cups_energy_drink")

    fun error(
        prompt: ApiReviewPrompt,
        values: Map<String, JsonElement>,
    ): String? {
        if (prompt.kind == PromptKind.Cups && values["caffeine_habit"] != JsonPrimitive("non_user") &&
            drinkFields.none { field -> values[field].numberValue?.let { it > BigDecimal.ZERO } == true }
        ) {
            return "Add at least one drink to save this answer."
        }
        if (prompt.kind == PromptKind.BlueBlockers && values["blue_blockers"] == JsonPrimitive("rated")) {
            val text = (values["blue_blocker_mdfd"] as? JsonPrimitive)?.content ?: ""
            val rating = text.replace(',', '.').toDoubleOrNull()
            if (rating == null || !rating.isFinite() ||
                rating !in 0.1..9.9
            ) {
                return "Enter a rating between 0.1 and 9.9."
            }
        }
        if (prompt.kind == PromptKind.FreeDayTimes && values["free_day_status"] == JsonPrimitive("declared") &&
            (
                !WizardDateTime.isTime(values["free_day_bed"].stringValue) ||
                    !WizardDateTime.isTime(values["free_day_wake"].stringValue)
            )
        ) {
            return "Check your bedtime and wake-up time."
        }
        return null
    }
}

internal val JsonElement?.numberValue: BigDecimal? get() =
    (this as? JsonPrimitive)
        ?.takeUnless {
            it.isString
        }?.content
        ?.toBigDecimalOrNull()
