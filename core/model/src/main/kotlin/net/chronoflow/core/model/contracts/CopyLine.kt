package net.chronoflow.core.model.contracts

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.math.BigDecimal

@Serializable(with = CopyArgValue.Serializer::class)
sealed interface CopyArgValue {
    data class Text(
        val value: String,
    ) : CopyArgValue

    data class Numeric(
        val value: BigDecimal,
    ) : CopyArgValue

    object Serializer : KSerializer<CopyArgValue> {
        override val descriptor: SerialDescriptor =
            SerialDescriptor("net.chronoflow.core.model.contracts.CopyArgValue", JsonElement.serializer().descriptor)

        override fun deserialize(decoder: Decoder): CopyArgValue {
            val element =
                (
                    decoder as? JsonDecoder ?: throw SerializationException(
                        "CopyArgValue supports JSON only",
                    )
                ).decodeJsonElement()
            if (element !is JsonPrimitive) throw SerializationException("CopyArgValue must be a string or a number")
            if (element.isString) return Text(element.content)
            val number =
                element.content.toBigDecimalOrNull()
                    ?: throw SerializationException("CopyArgValue must be a string or a number")
            return Numeric(number)
        }

        override fun serialize(
            encoder: Encoder,
            value: CopyArgValue,
        ) {
            val json = encoder as? JsonEncoder ?: throw SerializationException("CopyArgValue supports JSON only")
            json.encodeJsonElement(
                when (value) {
                    is Text -> JsonPrimitive(value.value)
                    is Numeric -> JsonPrimitive(value.value)
                },
            )
        }
    }
}

@Serializable(with = CopyArgument.Serializer::class)
sealed interface CopyArgument {
    data class Standard(
        val value: CopyArg,
    ) : CopyArgument

    data class Caffeine(
        val value: CaffeineCopyArg,
    ) : CopyArgument

    object Serializer : KSerializer<CopyArgument> {
        override val descriptor: SerialDescriptor =
            SerialDescriptor("net.chronoflow.core.model.contracts.CopyArgument", JsonElement.serializer().descriptor)

        override fun deserialize(decoder: Decoder): CopyArgument {
            val json = decoder as? JsonDecoder ?: throw SerializationException("CopyArgument supports JSON only")
            val element =
                json.decodeJsonElement() as? JsonObject
                    ?: throw SerializationException("CopyArgument must be an object")
            return if ("drinks" in element) {
                Caffeine(json.json.decodeFromJsonElement(CaffeineCopyArg.serializer(), element))
            } else {
                Standard(json.json.decodeFromJsonElement(CopyArg.serializer(), element))
            }
        }

        override fun serialize(
            encoder: Encoder,
            value: CopyArgument,
        ) {
            val json = encoder as? JsonEncoder ?: throw SerializationException("CopyArgument supports JSON only")
            json.encodeJsonElement(
                when (value) {
                    is Standard -> json.json.encodeToJsonElement(CopyArg.serializer(), value.value)
                    is Caffeine -> json.json.encodeToJsonElement(CaffeineCopyArg.serializer(), value.value)
                },
            )
        }
    }
}
