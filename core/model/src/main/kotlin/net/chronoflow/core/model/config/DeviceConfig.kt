package net.chronoflow.core.model.config

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

data class DeviceConfig(
    val language: LanguageChoice = LanguageChoice.System,
    val theme: ThemeChoice = ThemeChoice.System,
) {
    fun encode(): ByteArray = "language=${language.raw}\ntheme=${theme.raw}\n".toByteArray(Charsets.UTF_8)

    companion object {
        const val MAX_BYTES = 64

        fun decode(bytes: ByteArray?): DeviceConfig {
            if (bytes == null || bytes.size > MAX_BYTES) return DeviceConfig()
            val text =
                try {
                    Charsets.UTF_8
                        .newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(bytes))
                        .toString()
                } catch (_: CharacterCodingException) {
                    return DeviceConfig()
                }
            var language = LanguageChoice.System
            var theme = ThemeChoice.System
            for (line in text.split('\n')) {
                val separator = line.indexOf('=')
                if (separator < 0) continue
                val value = line.substring(separator + 1).trimEnd('\r')
                when (line.substring(0, separator)) {
                    "language" -> LanguageChoice.fromRaw(value)?.let { language = it }
                    "theme" -> ThemeChoice.fromRaw(value)?.let { theme = it }
                }
            }
            return DeviceConfig(language, theme)
        }
    }
}
