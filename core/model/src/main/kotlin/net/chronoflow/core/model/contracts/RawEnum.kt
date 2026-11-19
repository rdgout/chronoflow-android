package net.chronoflow.core.model.contracts

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

interface RawEnum {
    val raw: String
}

open class RawEnumSerializer<T : RawEnum>(
    serialName: String,
    private val of: (String) -> T,
) : KSerializer<T> {
    override val descriptor = PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: T,
    ) = encoder.encodeString(value.raw)

    override fun deserialize(decoder: Decoder): T = of(decoder.decodeString())
}
