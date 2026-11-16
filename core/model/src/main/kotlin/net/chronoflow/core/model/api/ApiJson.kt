package net.chronoflow.core.model.api

import kotlinx.serialization.json.Json

object ApiJson {
    val response: Json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    val request: Json =
        Json {
            explicitNulls = true
            encodeDefaults = false
        }
}
