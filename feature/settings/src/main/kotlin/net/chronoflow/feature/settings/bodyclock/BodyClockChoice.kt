package net.chronoflow.feature.settings.bodyclock

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

object BodyClockChoice {
    fun patch(
        answers: JsonObject,
        key: String,
        id: String,
    ): Map<String, JsonElement> =
        buildMap {
            put(key, if (id.isEmpty()) JsonNull else JsonPrimitive(id))
            if (key == "freeDayStatus" && id == "declared") {
                put(
                    "freeDayBed",
                    answers["freeDayBed"]?.takeUnless { it == JsonNull } ?: answers["habitualBed"] ?: JsonNull,
                )
                put(
                    "freeDayWake",
                    answers["freeDayWake"]?.takeUnless { it == JsonNull } ?: answers["habitualWake"] ?: JsonNull,
                )
            }
        }

    fun yesNoPatch(
        key: String,
        id: String,
    ): Map<String, JsonElement> = mapOf(key to JsonPrimitive(id == "yes"))

    fun yesNoSelection(value: JsonElement?): String =
        when (value) {
            JsonPrimitive(true) -> "yes"
            JsonPrimitive(false) -> "no"
            else -> ""
        }
}
