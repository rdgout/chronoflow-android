package net.chronoflow.core.model.config

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test
import java.util.Locale

class ConfigurationRulesTest {
    private fun system(tag: String) =
        ConfigurationRules.resolveLocale(LanguageChoice.System, Locale.forLanguageTag(tag))

    @Test
    fun fontScaleIsCappedAtOnePointThree() {
        assertThat(ConfigurationRules.capFontScale(0.85f)).isEqualTo(0.85f)
        assertThat(ConfigurationRules.capFontScale(1.0f)).isEqualTo(1.0f)
        assertThat(ConfigurationRules.capFontScale(1.3f)).isEqualTo(1.3f)
        assertThat(ConfigurationRules.capFontScale(2.0f)).isEqualTo(1.3f)
    }

    @Test
    fun invalidFontScaleFallsBackToOne() {
        assertThat(ConfigurationRules.capFontScale(Float.NaN)).isEqualTo(1.0f)
        assertThat(ConfigurationRules.capFontScale(0f)).isEqualTo(1.0f)
        assertThat(ConfigurationRules.capFontScale(-1f)).isEqualTo(1.0f)
    }

    @Test
    fun systemLocaleCoercesToEnglishOrDutchKeepingOurRegions() {
        assertThat(system("ar-EG").toLanguageTag()).isEqualTo("en")
        assertThat(system("de-DE").toLanguageTag()).isEqualTo("en")
        assertThat(system("nl-BE").toLanguageTag()).isEqualTo("nl-BE")
        assertThat(system("nl-NL").toLanguageTag()).isEqualTo("nl-NL")
        assertThat(system("en-GB").toLanguageTag()).isEqualTo("en-GB")
        assertThat(system("en-US-u-nu-arab").toLanguageTag()).isEqualTo("en-US")
        assertThat(system("nl").toLanguageTag()).isEqualTo("nl")
        assertThat(ConfigurationRules.resolveLocale(LanguageChoice.System, Locale.ROOT).toLanguageTag()).isEqualTo("en")
    }

    @Test
    fun explicitChoiceWinsAndKeepsOnlyAMatchingRegion() {
        val enUs = Locale.forLanguageTag("en-US")
        assertThat(ConfigurationRules.resolveLocale(LanguageChoice.Nl, enUs).toLanguageTag()).isEqualTo("nl")
        assertThat(ConfigurationRules.resolveLocale(LanguageChoice.En, Locale.forLanguageTag("nl-NL")).toLanguageTag())
            .isEqualTo("en")
        assertThat(ConfigurationRules.resolveLocale(LanguageChoice.Nl, Locale.forLanguageTag("nl-BE")).toLanguageTag())
            .isEqualTo("nl-BE")
    }

    @Test
    fun arabicPhoneFormatsLatinDigits() {
        assertThat(String.format(system("ar-EG"), "%,d", 1234567)).isEqualTo("1,234,567")
    }
}
