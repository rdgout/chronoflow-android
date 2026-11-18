package net.chronoflow.core.datamock

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Android crypto helper coverage (new). */
class MockCryptoTest {
    @Test fun sha256Vector() =
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", sha256Hex("abc"))

    @Test fun sha1Vector() = assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d", sha1Hex("abc"))

    @Test fun hmacVector() =
        assertEquals(
            "f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8",
            hmacSha256Hex("key".encodeToByteArray(), "The quick brown fox jumps over the lazy dog"),
        )

    @Test fun unpaddedBase64() = assertArrayEquals(byteArrayOf(0), base64URL("AA"))

    @Test fun paddedBase64() = assertArrayEquals(byteArrayOf(0), base64URL("AA=="))

    @Test fun trailingBitsMatchFoundation() = assertArrayEquals(byteArrayOf(0), base64URL("AB"))

    @Test fun shortBase64IsRejected() = assertNull(base64URL("A"))

    @Test fun invalidBase64IsRejected() = assertNull(base64URL("A*B"))

    @Test fun standardAlphabetMatchesFoundation() = assertArrayEquals(byteArrayOf(107, -26), base64URL("a+b"))

    @Test fun encodingUsesUnpaddedUrlAlphabet() {
        val bytes = ByteArray(32) { 255.toByte() }
        val encoded = base64Url(bytes)
        assertEquals(43, encoded.length)
        assertTrue(encoded.all { it in "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_" })
        assertArrayEquals(bytes, base64URL(encoded))
    }
}
