package net.chronoflow.core.model.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class UUIDv7Tests {
    @Test fun uuidv7Encodes48BitTimestampVersionVariantAndRandomness() {
        assertEquals(
            "01234567-89ab-7fff-bfff-ffffffffffff",
            UUIDv7
                .generate(
                    0x0123_4567_89ab,
                    ByteArray(10) {
                        0xff.toByte()
                    },
                ).toString(),
        )
        assertEquals("00000000-0000-7000-8000-000000000000", UUIDv7.generate(0, ByteArray(10)).toString())
        assertThrows(DeviceIdentityFailure.InvalidTimestamp::class.java) { UUIDv7.generate(1L shl 48, ByteArray(10)) }
        assertThrows(DeviceIdentityFailure.InvalidRandomness::class.java) { UUIDv7.generate(0, byteArrayOf()) }
    }

    @Test fun negativeTimestampIsRejected() {
        assertThrows(DeviceIdentityFailure.InvalidTimestamp::class.java) { UUIDv7.generate(-1, ByteArray(10)) }
    }
}
