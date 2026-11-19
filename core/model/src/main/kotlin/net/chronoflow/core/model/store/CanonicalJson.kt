package net.chronoflow.core.model.store

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

object CanonicalJson {
    fun encode(element: JsonElement): String =
        when (element) {
            is JsonObject -> JsonObject(element.toSortedMap().mapValues { normalize(it.value) }).toString()
            else -> normalize(element).toString()
        }

    private fun normalize(element: JsonElement): JsonElement =
        when (element) {
            is JsonObject -> JsonObject(element.toSortedMap().mapValues { normalize(it.value) })
            is JsonArray -> JsonArray(element.map(::normalize))
            else -> element
        }
}
