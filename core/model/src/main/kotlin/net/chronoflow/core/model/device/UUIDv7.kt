package net.chronoflow.core.model.device

import java.nio.ByteBuffer
import java.util.UUID

object UUIDv7 {
    fun generate(
        millisecondsSince1970: Long,
        randomBytes: ByteArray,
    ): UUID {
        if (millisecondsSince1970 !in 0 until (1L shl 48)) throw DeviceIdentityFailure.InvalidTimestamp
        if (randomBytes.size != 10) throw DeviceIdentityFailure.InvalidRandomness
        val bytes = ByteArray(16)
        repeat(6) { bytes[it] = (millisecondsSince1970 ushr ((5 - it) * 8)).toByte() }
        bytes[6] = (0x70 or (randomBytes[0].toInt() and 0x0f)).toByte()
        bytes[7] = randomBytes[1]
        bytes[8] = (0x80 or (randomBytes[2].toInt() and 0x3f)).toByte()
        randomBytes.copyInto(bytes, 9, 3)
        val buffer = ByteBuffer.wrap(bytes)
        return UUID(buffer.long, buffer.long)
    }
}
