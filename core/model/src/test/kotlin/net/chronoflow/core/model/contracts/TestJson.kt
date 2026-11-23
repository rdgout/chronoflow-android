package net.chronoflow.core.model.contracts

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.testing.Fixtures

internal fun fixtureObject(path: String): JsonObject =
    ApiJson.response.parseToJsonElement(Fixtures.text(path)) as JsonObject

internal fun JsonObject.with(
    key: String,
    value: JsonElement?,
): JsonObject = JsonObject(if (value == null) this - key else this + (key to value))

internal fun removingNulls(value: JsonElement): JsonElement =
    when (value) {
        is JsonObject -> JsonObject(value.filterValues { it != JsonNull }.mapValues { removingNulls(it.value) })
        is JsonArray -> JsonArray(value.map { removingNulls(it) })
        else -> value
    }
