package net.chronoflow.core.wizard.model

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.api.ApiTripReviewInput

object WizardFingerprint {
    fun canonical(input: ApiTripReviewInput): String =
        sorted(ApiJson.request.encodeToJsonElement(ApiTripReviewInput.serializer(), input)).toString()

    private fun sorted(value: JsonElement): JsonElement =
        when (value) {
            is JsonObject -> JsonObject(value.toSortedMap().mapValues { sorted(it.value) })
            is JsonArray -> JsonArray(value.map(::sorted))
            else -> value
        }
}
