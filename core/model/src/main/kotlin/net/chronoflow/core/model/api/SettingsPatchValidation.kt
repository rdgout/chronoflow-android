package net.chronoflow.core.model.api

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.model.contracts.ProfilePreferences

object SettingsPatchValidation {
    fun validate(
        section: SettingsSection,
        patch: JsonObject,
    ) {
        val allowed =
            when (section) {
                SettingsSection.Preferences -> setOf("time_format", "hidden_categories", "show_optional_items")
                SettingsSection.Reminders -> setOf("light", "avoid", "caffeine", "winddown", "lead_min")
                SettingsSection.BodyClock -> ProfilePreferences.requestFields
            }
        for (key in listOf("exclude_caffeine", "flight_movement_breaks")) {
            if (key !in patch) continue
            val value = patch[key]
            if (value !is JsonPrimitive || value.isString || value.booleanOrNull == null) {
                throw DataFailure.Validation(mapOf(key to FieldMessage("Choose one of the available options.")), 422)
            }
        }
        if (patch.isEmpty() || patch.keys.any { it !in allowed }) {
            throw DataFailure.Validation(mapOf("form" to FieldMessage("This setting cannot be changed here.")), 422)
        }
    }
}
