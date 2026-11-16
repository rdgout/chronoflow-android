package net.chronoflow.core.model.contracts

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.api.ApiJson

object ProfilePreferences {
    val requestFields: Set<String> =
        setOf(
            "habitual_bed",
            "habitual_wake",
            "free_day_status",
            "free_day_bed",
            "free_day_wake",
            "chronotype",
            "age_band",
            "shift_work",
            "exclude_caffeine",
            "flight_movement_breaks",
            "caffeine_habit",
            "cups_filter_coffee",
            "cups_tea",
            "cups_cola",
            "cups_energy_drink",
            "caffeine_sensitive",
            "caffeine_non_user_opt_in",
            "light_device",
            "blue_blockers",
            "blue_blocker_mdfd",
        )

    private val retiredFields: Set<String> =
        setOf(
            "bipolar",
            "eye_disease",
            "eyeDisease",
            "pregnancy",
            "vte",
            "sleep_phase_disorder",
            "sleepPhaseDisorder",
            "oral_contraception",
            "oralContraception",
            "caffeine_ruled_out",
            "caffeineRuledOut",
        )

    private val yes = JsonPrimitive(true)

    fun camel(key: String): String {
        val parts = key.split("_").filter { it.isNotEmpty() }
        return (parts.firstOrNull() ?: "") +
            parts.drop(1).joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
    }

    fun bodyClock(value: JsonObject): JsonObject {
        val allowed = requestFields.map(::camel).toSet()
        val result = value.filterKeys { it in allowed }.toMutableMap()
        result["excludeCaffeine"] =
            JsonPrimitive(
                value["excludeCaffeine"] == yes || ("excludeCaffeine" !in value && value["caffeineRuledOut"] == yes),
            )
        result["flightMovementBreaks"] = JsonPrimitive(value["flightMovementBreaks"] == yes)
        result["invalidDrafts"] =
            JsonObject(
                (value["invalidDrafts"] as? JsonObject ?: JsonObject(emptyMap())).filterKeys {
                    it in
                        requestFields
                },
            )
        return JsonObject(result)
    }

    fun request(value: JsonObject): JsonObject {
        val result = promptAnswers(value).filterKeys { it in requestFields }.toMutableMap()
        for (key in listOf("exclude_caffeine", "flight_movement_breaks")) {
            if (key in result) result[key] = JsonPrimitive(result[key] == yes)
        }
        return JsonObject(result)
    }

    fun promptAnswers(values: Map<String, JsonElement>): Map<String, JsonElement> =
        (cleanLegacyValue(JsonObject(values)) as? JsonObject) ?: JsonObject(emptyMap())

    @JvmName("promptAnswersOfPrimitives")
    fun promptAnswers(values: Map<String, JsonPrimitive>): Map<String, JsonPrimitive> =
        promptAnswers(values as Map<String, JsonElement>).mapValues { it.value as JsonPrimitive }

    fun reviewPrompts(prompts: List<ApiReviewPrompt>): List<ApiReviewPrompt> = prompts.filter { isSupported(it) }

    fun isSupported(prompt: ApiReviewPrompt): Boolean {
        if (prompt.kind ==
            PromptKind.Screening
        ) {
            return prompt.target == ReviewPromptTarget.Profile &&
                prompt.fields == listOf("shift_work")
        }
        if (prompt.kind ==
            PromptKind.AgeBand
        ) {
            return prompt.target == ReviewPromptTarget.Profile &&
                prompt.fields == listOf("age_band")
        }
        return prompt.target != ReviewPromptTarget.Profile || requestFields.containsAll(prompt.fields)
    }

    fun isSupported(prompt: PromptView): Boolean =
        prompt.kind != PromptKind.Screening || prompt.question == ScreeningQuestion.ShiftWork

    fun readiness(
        value: JsonObject,
        bodyClock: JsonObject,
    ): JsonObject {
        val missing: List<JsonElement> =
            listOf("ageBand" to JsonNull, "shiftWork" to JsonPrimitive("shift_work")).mapNotNull { (key, question) ->
                val answer = bodyClock[key]
                if (answer == null || answer == JsonNull || answer == JsonPrimitive("unanswered")) {
                    JsonObject(mapOf("question" to question))
                } else {
                    null
                }
            }
        val underAge = bodyClock["ageBand"] == JsonPrimitive("under_18")
        val shiftWork = bodyClock["shiftWork"] == yes
        val excluded = underAge || shiftWork
        val adjustments =
            adjustmentKeys(bodyClock).map {
                JsonObject(
                    mapOf(
                        "key" to JsonPrimitive(it),
                        "args" to JsonArray(emptyList()),
                    ),
                )
            }
        val invalid = (value["invalidSections"] as? JsonArray)?.toList() ?: emptyList()
        val variant =
            when {
                excluded -> "excluded"
                missing.isNotEmpty() || invalid.isNotEmpty() -> "needs-answers"
                adjustments.isEmpty() -> "ready"
                else -> "adjusted"
            }
        val exclusion =
            if (underAge) {
                "This plan is not available for travellers under 18."
            } else {
                "This plan is not available if, in the past 14 days, you have worked night or rotating shifts or had a very irregular sleep schedule."
            }
        return JsonObject(
            mapOf(
                "invalidSections" to JsonArray(invalid),
                "variant" to JsonPrimitive(variant),
                "missing" to JsonArray(if (excluded) emptyList() else missing),
                "exclusionTarget" to
                    if (excluded) {
                        JsonObject(mapOf("question" to if (underAge) JsonNull else JsonPrimitive("shift_work")))
                    } else {
                        JsonNull
                    },
                "exclusions" to
                    JsonArray(
                        if (excluded) {
                            listOf(
                                JsonObject(mapOf("key" to JsonPrimitive(exclusion), "args" to JsonArray(emptyList()))),
                            )
                        } else {
                            emptyList()
                        },
                    ),
                "adjustments" to JsonArray(adjustments),
            ),
        )
    }

    fun containsRetiredValues(json: String): Boolean =
        retiredFields.any { json.contains("\"" + it + "\"") } ||
            json.contains("Answer the health questions to get your plan.")

    fun cleanLegacyJSON(data: ByteArray): ByteArray {
        if (!containsRetiredValues(data.decodeToString())) return data
        val original = ApiJson.response.parseToJsonElement(data.decodeToString())
        val cleaned = cleanLegacyValue(original) ?: JsonNull
        return if (cleaned ==
            original
        ) {
            data
        } else {
            ApiJson.response.encodeToString(JsonElement.serializer(), cleaned).encodeToByteArray()
        }
    }

    private fun string(value: JsonElement?): String? = (value as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun cleanLegacyValue(value: JsonElement): JsonElement? =
        when (value) {
            is JsonObject -> cleanLegacyObject(value)
            is JsonArray -> JsonArray(value.mapNotNull { cleanLegacyValue(it) })
            else -> value
        }

    private fun cleanLegacyObject(fields: JsonObject): JsonElement? {
        val question = string(fields["question"])
        if (question != null && question in retiredFields) return null
        val keys = fields["fields"] as? JsonArray
        if (fields["kind"] == JsonPrimitive("screening") && keys != null &&
            keys.any { (string(it) ?: "") in retiredFields }
        ) {
            return null
        }
        val result = LinkedHashMap<String, JsonElement>()
        for ((key, entry) in fields) {
            if (key in retiredFields) continue
            cleanLegacyValue(entry)?.let { result[key] = it }
        }
        if (result["key"] == JsonPrimitive("Answer the health questions to get your plan.")) {
            result["key"] = JsonPrimitive("Answer the questions to get your plan.")
        }
        return JsonObject(result)
    }

    private fun adjustmentKeys(body: JsonObject): List<String> {
        val keys = mutableListOf<String>()
        if (body["excludeCaffeine"] == yes) keys.add("No caffeine anywhere in your plan.")
        if (body["flightMovementBreaks"] == yes) {
            keys.add("Regular movement breaks on long flights, with shorter in-flight sleep stretches.")
        }
        if (body["ageBand"] ==
            JsonPrimitive("65_plus")
        ) {
            keys.add("Your plan takes your age range into account in its timing.")
        }
        return keys
    }
}
