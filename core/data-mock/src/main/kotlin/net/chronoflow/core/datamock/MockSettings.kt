package net.chronoflow.core.datamock

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.model.api.AccountSettings
import net.chronoflow.core.model.api.SettingsPatchValidation
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.ProfilePreferences

internal val JsonElement?.string: String? get() = (this as? JsonPrimitive)?.takeIf { it.isString }?.content
internal val JsonElement?.number: Double? get() = (this as? JsonPrimitive)?.takeIf { !it.isString }?.doubleOrNull
internal val JsonElement?.bool: Boolean? get() = (this as? JsonPrimitive)?.takeIf { !it.isString }?.booleanOrNull

object MockSettings {
    fun camel(key: String): String {
        val components = key.split('_').filter { it.isNotEmpty() }
        return components.first() +
            components.drop(1).joinToString("") { it.take(1).uppercase(java.util.Locale.ROOT) + it.drop(1) }
    }

    fun invalid(
        field: String,
        key: String = "Choose one of the available options.",
    ): DataFailure = DataFailure.Validation(mapOf(field to FieldMessage(key)), 422)

    fun validate(
        section: SettingsSection,
        patch: JsonObject,
        current: AccountSettings,
    ) {
        SettingsPatchValidation.validate(section, patch)
        val fields = linkedMapOf<String, FieldMessage>()
        for (key in patch.keys.sorted()) {
            val value = patch.getValue(key)
            val valid =
                when (section) {
                    SettingsSection.Preferences -> {
                        when (key) {
                            "time_format" -> {
                                value.string in listOf("12", "24")
                            }

                            "show_optional_items" -> {
                                value.bool != null
                            }

                            else -> {
                                if (value is JsonArray) {
                                    val types =
                                        value.mapNotNull {
                                            it.string?.let(BlockType::of)?.takeUnless { type ->
                                                type is BlockType.Unknown
                                            }
                                        }
                                    types.size == value.size && types.toSet().size == value.size
                                } else {
                                    false
                                }
                            }
                        }
                    }

                    SettingsSection.Reminders -> {
                        if (key ==
                            "lead_min"
                        ) {
                            value.number in listOf(15.0, 30.0, 60.0)
                        } else {
                            value.bool != null
                        }
                    }

                    SettingsSection.BodyClock -> {
                        bodyValue(key, value)
                    }
                }
            if (!valid) {
                fields[key] =
                    FieldMessage(
                        if (key.contains("bed") ||
                            key.contains("wake")
                        ) {
                            "Enter a valid time."
                        } else {
                            "Choose one of the available options."
                        },
                    )
            }
        }
        if (fields.isNotEmpty()) throw DataFailure.Validation(fields, 422)
        if (section == SettingsSection.BodyClock) {
            val merged = current.bodyClock + patch.mapKeys { camel(it.key) }
            for ((bed, wake, field) in listOf(
                Triple("habitualBed", "habitualWake", "habitual_bed"),
                Triple("freeDayBed", "freeDayWake", "free_day_bed"),
            )) {
                if (bed == "freeDayBed" && merged["freeDayStatus"].string != "declared") continue
                val bedTime = merged[bed].string
                val wakeTime = merged[wake].string
                if (bedTime != null && wakeTime != null) {
                    fun minutes(time: String): Int? {
                        val parts = time.split(':').mapNotNull { it.toIntOrNull() }
                        if (parts.size != 2) return null
                        return parts[0] * 60 + parts[1]
                    }
                    val wakeMinutes = minutes(wakeTime) ?: throw invalid(field, "Check your bedtime and wake-up time.")
                    val bedMinutes = minutes(bedTime) ?: throw invalid(field, "Check your bedtime and wake-up time.")
                    if ((wakeMinutes - bedMinutes + 1440) % 1440 !in
                        180..840
                    ) {
                        throw invalid(field, "Check your bedtime and wake-up time.")
                    }
                }
            }
            if (merged["freeDayStatus"].string == "declared" &&
                (merged["freeDayBed"].string == null || merged["freeDayWake"].string == null)
            ) {
                throw invalid("free_day_bed", "Check your bedtime and wake-up time.")
            }
            if (merged["caffeineHabit"].string != "cups") {
                for (key in listOf("cups_filter_coffee", "cups_tea", "cups_cola", "cups_energy_drink")) {
                    if ((patch[key].number ?: 0.0) >
                        0
                    ) {
                        throw invalid(
                            key,
                            "Add your usual drinks per day, or leave your caffeine habits unanswered.",
                        )
                    }
                }
            }
            if (merged["caffeineHabit"].string == "cups") {
                val counts =
                    listOf(
                        "cupsFilterCoffee",
                        "cupsTea",
                        "cupsCola",
                        "cupsEnergyDrink",
                    ).mapNotNull { merged[it].number }
                if (counts.sum() ==
                    0.0
                ) {
                    throw invalid("cups_filter_coffee", "Add at least one drink to save this answer.")
                }
            }
            if (merged["blueBlockers"].string == "rated" &&
                merged["blueBlockerMdfd"] == JsonNull
            ) {
                throw invalid("blue_blocker_mdfd", "Enter a rating between 0.1 and 9.9.")
            }
        }
    }

    private val timePattern = Regex("^(?:[01][0-9]|2[0-3]):[0-5][0-9]$")
    private val ratingPattern = Regex("^[0-9]+(?:\\.[0-9])?$")
    private val choices =
        mapOf(
            "free_day_status" to listOf("unconfirmed", "same_as_workdays", "declared"),
            "chronotype" to listOf("early", "intermediate", "late"),
            "age_band" to listOf("under_18", "18_64", "65_plus"),
            "caffeine_habit" to listOf("non_user", "cups"),
            "light_device" to listOf("none", "box", "glasses"),
            "blue_blockers" to listOf("no", "rated", "unrated"),
        )

    private fun bodyValue(
        key: String,
        value: JsonElement,
    ): Boolean {
        if (key in listOf("habitual_bed", "habitual_wake", "free_day_bed", "free_day_wake")) {
            if (value == JsonNull) return key.startsWith("free_day_")
            return value.string?.let(timePattern::matches) == true
        }
        choices[key]?.let { options ->
            return (value == JsonNull && key != "free_day_status") || value.string in options
        }
        if (key.startsWith("cups_")) {
            if (value == JsonNull) return true
            val number = value.number ?: return false
            return number in 0.0..20.0 && number == number.toInt().toDouble()
        }
        if (key == "blue_blocker_mdfd") {
            if (value == JsonNull) return true
            val text =
                value.number?.let {
                    (value as JsonPrimitive)
                        .content
                        .toBigDecimal()
                        .stripTrailingZeros()
                        .toPlainString()
                }
                    ?: value.string
                    ?: return false
            val rating = text.toDoubleOrNull() ?: return false
            return rating in 0.1..9.9 && ratingPattern.matches(text)
        }
        if (value.bool != null) return true
        return value == JsonNull &&
            key !in
            listOf("caffeine_sensitive", "caffeine_non_user_opt_in", "exclude_caffeine", "flight_movement_breaks")
    }

    fun apply(
        patch: JsonObject,
        section: SettingsSection,
        settings: AccountSettings,
    ): AccountSettings {
        val body = settings.bodyClock.toMutableMap()
        val preferences = settings.preferences.toMutableMap()
        val reminders = settings.reminders.toMutableMap()
        for ((key, value) in patch) {
            when (section) {
                SettingsSection.BodyClock -> {
                    body[camel(key)] =
                        if (key == "blue_blocker_mdfd" &&
                            value.number != null
                        ) {
                            JsonPrimitive(
                                (value as JsonPrimitive)
                                    .content
                                    .toBigDecimal()
                                    .stripTrailingZeros()
                                    .toPlainString(),
                            )
                        } else {
                            value
                        }
                }

                SettingsSection.Preferences -> {
                    preferences[camel(key)] = value
                }

                SettingsSection.Reminders -> {
                    reminders[camel(key)] = value
                }
            }
        }
        var sleepAnswered = settings.sleepAnswered
        if (section == SettingsSection.BodyClock) {
            if (patch.keys.any {
                    it in
                        listOf("habitual_bed", "habitual_wake", "free_day_status", "free_day_bed", "free_day_wake")
                }
            ) {
                sleepAnswered =
                    true
            }
            if (body["freeDayStatus"].string != "declared") {
                body["freeDayBed"] = JsonNull
                body["freeDayWake"] = JsonNull
            }
            if (body["blueBlockers"].string != "rated") body["blueBlockerMdfd"] = JsonNull
        }
        return AccountSettings(
            JsonObject(body),
            sleepAnswered,
            JsonObject(preferences),
            JsonObject(reminders),
            settings.planCredits,
            settings.policies,
        )
    }

    fun readiness(body: JsonObject): JsonObject =
        ProfilePreferences.readiness(
            JsonObject(
                mapOf("invalidSections" to JsonArray(emptyList())),
            ),
            body,
        )
}
