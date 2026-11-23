package net.chronoflow.core.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = AccountID.Serializer::class)
@JvmInline
value class AccountID private constructor(
    val rawValue: String,
) {
    class InvalidUUID : IllegalArgumentException("invalid account id")

    object Serializer : KSerializer<AccountID> {
        override val descriptor = PrimitiveSerialDescriptor("net.chronoflow.core.model.AccountID", PrimitiveKind.STRING)

        override fun serialize(
            encoder: Encoder,
            value: AccountID,
        ) = encoder.encodeString(value.rawValue)

        override fun deserialize(decoder: Decoder): AccountID = of(decoder.decodeString())
    }

    companion object {
        private val pattern = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

        // java.util.UUID.fromString accepts "1-1-1-1-1", which iOS UUID(uuidString:) rejects.
        fun of(value: String): AccountID {
            if (value.length != 36 || !pattern.matches(value)) throw InvalidUUID()
            return AccountID(value.lowercase())
        }
    }
}
