package net.chronoflow.core.sync

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.CheckStatus

@Serializable
data class SyncRefusal(
    val id: String,
    val kind: String,
    val status: Int? = null,
    val failedAt: String,
    val lastError: String,
    val notice: SyncRefusalNotice? = null,
)

@Serializable(with = SyncRefusalNotice.Serializer::class)
sealed interface SyncRefusalNotice {
    data object SleepTimes : SyncRefusalNotice

    data class StepDone(
        val type: BlockType,
    ) : SyncRefusalNotice

    companion object {
        fun from(change: OutboxChange): SyncRefusalNotice? =
            when (change) {
                is OutboxChange.Settings -> {
                    if (change.section == SettingsSection.BodyClock && change.patch.isNotEmpty() &&
                        change.patch.keys.all {
                            it in
                                setOf(
                                    "habitual_bed",
                                    "habitual_wake",
                                    "free_day_bed",
                                    "free_day_wake",
                                    "free_day_status",
                                )
                        }
                    ) {
                        SleepTimes
                    } else {
                        null
                    }
                }

                is OutboxChange.CheckOff -> {
                    if (change.change.status == CheckStatus.Done && change.change.type !is BlockType.Unknown) {
                        StepDone(change.change.type)
                    } else {
                        null
                    }
                }

                else -> {
                    null
                }
            }
    }

    object Serializer : KSerializer<SyncRefusalNotice> {
        override val descriptor: SerialDescriptor = JsonObject.serializer().descriptor

        override fun serialize(
            encoder: Encoder,
            value: SyncRefusalNotice,
        ) {
            val json =
                when (value) {
                    SleepTimes -> {
                        JsonObject(mapOf("sleepTimes" to JsonObject(emptyMap())))
                    }

                    is StepDone -> {
                        val body = JsonObject(mapOf("_0" to JsonPrimitive(value.type.raw)))
                        JsonObject(mapOf("stepDone" to body))
                    }
                }
            (encoder as JsonEncoder).encodeJsonElement(json)
        }

        override fun deserialize(decoder: Decoder): SyncRefusalNotice {
            val element = (decoder as JsonDecoder).decodeJsonElement()
            val json = element as? JsonObject ?: throw SerializationException("Invalid refusal notice")
            if (json.size != 1) throw SerializationException("Invalid refusal notice")
            if (json["sleepTimes"] is JsonObject) return SleepTimes
            val step = json["stepDone"] as? JsonObject
            val primitive = step?.get("_0") as? JsonPrimitive
            if (primitive == null || !primitive.isString) throw SerializationException("Invalid refusal notice")
            return StepDone(BlockType.of(primitive.content))
        }
    }
}
