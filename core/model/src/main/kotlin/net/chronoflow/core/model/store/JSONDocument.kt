package net.chronoflow.core.model.store

import kotlinx.serialization.SerializationException
import net.chronoflow.core.model.api.ApiJson

object JSONDocument {
    fun requireValid(text: String): String {
        try {
            ApiJson.response.parseToJsonElement(text)
        } catch (_: SerializationException) {
            throw IllegalArgumentException("invalid JSON")
        }
        return text
    }
}
