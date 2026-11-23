package net.chronoflow.core.model.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ApiErrorEnvelope(
    // A String, not an enum: unknown codes must decode.
    val code: String,
    val message: String,
    val errors: Map<String, String>? = null,
    // 4b validates "strings or numbers only".
    val args: Map<String, Map<String, JsonElement>>? = null,
    val retryAfterSeconds: JsonElement? = null,
)
