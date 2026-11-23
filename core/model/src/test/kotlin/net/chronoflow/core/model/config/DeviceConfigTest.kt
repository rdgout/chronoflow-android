package net.chronoflow.core.model.config

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class DeviceConfigTest {
    private fun decode(text: String) = DeviceConfig.decode(text.toByteArray())

    @Test
    fun everyCombinationRoundTrips() {
        for (language in LanguageChoice.entries) {
            for (theme in ThemeChoice.entries) {
                val config = DeviceConfig(language, theme)
                assertThat(DeviceConfig.decode(config.encode())).isEqualTo(config)
            }
        }
    }

    @Test
    fun encodedFormIsFixed() {
        assertThat(DeviceConfig(LanguageChoice.Nl, ThemeChoice.Dark).encode().decodeToString())
            .isEqualTo("language=nl\ntheme=dark\n")
    }

    @Test
    fun sixtyFourBytesDecodeAndSixtyFiveDoNot() {
        val padding = " ".repeat(64 - "language=nl\n".length)
        assertThat(decode("language=nl\n$padding").language).isEqualTo(LanguageChoice.Nl)
        assertThat(decode("language=nl\n$padding ")).isEqualTo(DeviceConfig())
    }

    @Test
    fun lastValidValueWinsAndInvalidValuesAreSkipped() {
        assertThat(decode("language=nl\nlanguage=xx\n").language).isEqualTo(LanguageChoice.Nl)
    }

    @Test
    fun lenientAboutLineEndings() {
        assertThat(decode("theme=dark").theme).isEqualTo(ThemeChoice.Dark)
        assertThat(decode("language=nl\r\n").language).isEqualTo(LanguageChoice.Nl)
    }

    @Test
    fun unknownKeysAreIgnored() {
        assertThat(decode("colour=red\nlanguage=en\n")).isEqualTo(DeviceConfig(LanguageChoice.En))
    }

    @Test
    fun malformedBytesAndNullGiveDefaults() {
        assertThat(DeviceConfig.decode(byteArrayOf(0xC3.toByte(), 0x28))).isEqualTo(DeviceConfig())
        assertThat(DeviceConfig.decode(null)).isEqualTo(DeviceConfig())
    }
}
