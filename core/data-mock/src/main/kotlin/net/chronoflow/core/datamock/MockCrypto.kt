package net.chronoflow.core.datamock

import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private const val HEX_DIGITS = "0123456789abcdef"

internal fun ByteArray.hex(): String =
    buildString(size * 2) {
        for (byte in this@hex) {
            val value = byte.toInt() and 255
            append(HEX_DIGITS[value ushr 4])
            append(HEX_DIGITS[value and 15])
        }
    }

internal fun sha256Bytes(value: String): ByteArray =
    MessageDigest.getInstance("SHA-256").digest(value.encodeToByteArray())

internal fun sha256Hex(value: String): String = sha256Bytes(value).hex()

internal fun sha1Hex(value: String): String = MessageDigest.getInstance("SHA-1").digest(value.encodeToByteArray()).hex()

internal fun hmacSha256Hex(
    key: ByteArray,
    message: String,
): String =
    Mac.getInstance("HmacSHA256").run {
        init(SecretKeySpec(key, "HmacSHA256"))
        doFinal(message.encodeToByteArray()).hex()
    }

internal fun base64Url(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

internal fun base64URL(value: String): ByteArray? {
    val normalized = value.replace('-', '+').replace('_', '/')
    return try {
        Base64.getDecoder().decode(normalized + "=".repeat((4 - normalized.length % 4) % 4))
    } catch (_: IllegalArgumentException) {
        null
    }
}
