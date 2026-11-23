package net.chronoflow.app

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import net.chronoflow.app.config.DeviceConfigFile
import net.chronoflow.core.model.config.ConfigurationRules
import net.chronoflow.core.model.config.LanguageChoice
import java.util.Locale

/** Caps the font scale, coerces the locale to en or nl and forces left-to-right, whatever the phone says. */
object ConfigurationPolicy {
    fun overrideConfiguration(
        base: Context,
        sdkInt: Int = Build.VERSION.SDK_INT,
    ): Configuration {
        val current = base.resources.configuration
        // From API 33 LocaleManager already put an explicit app language into the configuration.
        val choice = if (sdkInt >= 33) LanguageChoice.System else DeviceConfigFile.readForLaunch(base, sdkInt).language
        return Configuration().apply {
            fontScale = ConfigurationRules.capFontScale(current.fontScale)
            setLocales(LocaleList(ConfigurationRules.resolveLocale(choice, current.locales[0])))
            // After setLocales, which also derives a layout direction.
            setLayoutDirection(Locale.ENGLISH)
        }
    }

    /** For non-activity contexts; activities use applyOverrideConfiguration in attachBaseContext. */
    fun wrap(context: Context): Context = context.createConfigurationContext(overrideConfiguration(context))
}
