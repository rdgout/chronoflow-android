package net.chronoflow.core.model.config

import java.util.Locale

object ConfigurationRules {
    const val MAX_FONT_SCALE = 1.3f

    fun capFontScale(system: Float): Float =
        if (!system.isFinite() ||
            system <= 0f
        ) {
            1.0f
        } else {
            minOf(system, MAX_FONT_SCALE)
        }

    fun resolveLanguage(
        choice: LanguageChoice,
        systemLocale: Locale,
    ): AppLanguage =
        when (choice) {
            LanguageChoice.En -> AppLanguage.En
            LanguageChoice.Nl -> AppLanguage.Nl
            LanguageChoice.System -> if (systemLocale.language == "nl") AppLanguage.Nl else AppLanguage.En
        }

    /** Keeps the phone's region only when its language is the resolved one, so pickers follow the phone's week start. */
    fun resolveLocale(
        choice: LanguageChoice,
        systemLocale: Locale,
    ): Locale {
        val language = resolveLanguage(choice, systemLocale)
        val region = if (systemLocale.language == language.tag) systemLocale.country else ""
        return Locale
            .Builder()
            .setLanguage(language.tag)
            .setRegion(region)
            .build()
    }
}
