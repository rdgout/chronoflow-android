package net.chronoflow.core.reminders

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.TimeFormat

data class ReminderSettings(
    val light: Boolean = true,
    val avoid: Boolean = true,
    val caffeine: Boolean = true,
    val winddown: Boolean = true,
    val leadMin: Int = 30,
) {
    val enabled: Boolean get() = light || avoid || caffeine || winddown

    fun settingEnabled(enabled: Boolean): ReminderSettings =
        ReminderSettings(enabled, enabled, enabled, enabled, leadMin)

    companion object {
        fun isLeadOption(minutes: Int): Boolean = minutes == 15 || minutes == 30 || minutes == 60

        fun decode(json: String?): ReminderSettings? {
            val value = reminderJsonObject(json) ?: return null

            fun flag(key: String): Boolean? = (value[key] as? JsonPrimitive)?.takeUnless { it.isString }?.booleanOrNull
            return ReminderSettings(
                light = flag("light") ?: return null,
                avoid = flag("avoid") ?: return null,
                caffeine = flag("caffeine") ?: return null,
                winddown = flag("winddown") ?: return null,
                leadMin = (value["leadMin"] as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull ?: return null,
            )
        }

        fun enabledPatch(enabled: Boolean): JsonObject =
            buildJsonObject {
                put("light", enabled)
                put("avoid", enabled)
                put("caffeine", enabled)
                put("winddown", enabled)
            }

        fun leadPatch(minutes: Int): JsonObject? =
            if (isLeadOption(minutes)) buildJsonObject { put("lead_min", minutes) } else null
    }
}

fun reminderTimeFormat(preferencesJson: String?): TimeFormat {
    val format = reminderJsonObject(preferencesJson)?.get("timeFormat") as? JsonPrimitive
    return if (format?.isString == true && format.content == "12") TimeFormat.Value12 else TimeFormat.Value24
}

private fun reminderJsonObject(json: String?): JsonObject? =
    try {
        json?.let { ApiJson.response.parseToJsonElement(it) as? JsonObject }
    } catch (_: SerializationException) {
        null
    }

enum class ReminderPermission {
    Undetermined,
    Granted,
    Blocked,
    Unsupported,
    ;

    fun remindersActive(settings: ReminderSettings?): Boolean = this == Granted && settings?.enabled == true
}

enum class ReminderKind(
    val raw: String,
) {
    SeekLight("seek_light"),
    AvoidLight("avoid_light"),
    CaffeineCutoff("caffeine_cutoff"),
    WindDown("wind_down"),
    ;

    companion object {
        fun of(raw: String): ReminderKind? = entries.firstOrNull { it.raw == raw }
    }
}
