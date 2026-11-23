package net.chronoflow.core.model.auth

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

internal fun percentDecodeOrNull(value: String): String? {
    val bytes = ByteArrayOutputStream()
    var index = 0
    while (index < value.length) {
        if (value[index] == '%') {
            if (index + 2 >= value.length) return null
            val high = value[index + 1].digitToIntOrNull(16) ?: return null
            val low = value[index + 2].digitToIntOrNull(16) ?: return null
            bytes.write(high * 16 + low)
            index += 3
        } else {
            val end = value.indexOf('%', index).let { if (it < 0) value.length else it }
            bytes.write(value.substring(index, end).toByteArray(Charsets.UTF_8))
            index = end
        }
    }
    return try {
        Charsets.UTF_8
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes.toByteArray()))
            .toString()
    } catch (_: CharacterCodingException) {
        null
    }
}
