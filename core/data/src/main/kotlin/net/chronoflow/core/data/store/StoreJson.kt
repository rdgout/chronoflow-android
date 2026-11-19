package net.chronoflow.core.data.store

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.store.CanonicalJson

internal fun json(text: String): JsonElement = ApiJson.response.parseToJsonElement(text)

internal fun objectValue(text: String): JsonObject = ApiJson.response.decodeFromString(JsonObject.serializer(), text)

internal fun encoded(value: JsonElement): String = CanonicalJson.encode(value)
