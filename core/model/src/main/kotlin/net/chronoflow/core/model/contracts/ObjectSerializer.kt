package net.chronoflow.core.model.contracts

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** A serializer that reads and writes one JSON object by hand; every malformed input is a SerializationException. */
abstract class ObjectSerializer<T>(
    serialName: String,
) : KSerializer<T> {
    override val descriptor: SerialDescriptor = SerialDescriptor(serialName, JsonObject.serializer().descriptor)

    abstract fun decode(
        json: Json,
        obj: JsonObject,
    ): T

    abstract fun encode(
        json: Json,
        value: T,
    ): JsonObject

    final override fun deserialize(decoder: Decoder): T {
        val json =
            decoder as? JsonDecoder ?: throw SerializationException("${descriptor.serialName} supports JSON only")
        val element =
            json.decodeJsonElement() as? JsonObject
                ?: throw SerializationException("${descriptor.serialName} must be an object")
        return decode(json.json, element)
    }

    final override fun serialize(
        encoder: Encoder,
        value: T,
    ) {
        val json =
            encoder as? JsonEncoder ?: throw SerializationException("${descriptor.serialName} supports JSON only")
        json.encodeJsonElement(encode(json.json, value))
    }
}

fun JsonObject.require(key: String): JsonElement = this[key] ?: throw SerializationException("missing key: $key")

fun JsonObject.stringAt(key: String): String =
    (require(key) as? JsonPrimitive)?.takeIf { it.isString }?.content
        ?: throw SerializationException("$key must be a string")

fun JsonObject.optionalStringAt(key: String): String? =
    when (val value = this[key]) {
        null -> {
            null
        }

        is JsonPrimitive -> {
            if (value.isString) {
                value.content
            } else if (value.content ==
                "null"
            ) {
                null
            } else {
                throw SerializationException("$key must be a string")
            }
        }

        else -> {
            throw SerializationException("$key must be a string")
        }
    }

fun JsonObject.booleanAt(key: String): Boolean =
    (require(key) as? JsonPrimitive)?.takeIf { !it.isString }?.content?.toBooleanStrictOrNull()
        ?: throw SerializationException("$key must be a boolean")

fun JsonObject.objectAt(key: String): JsonObject =
    require(key) as? JsonObject ?: throw SerializationException("$key must be an object")
