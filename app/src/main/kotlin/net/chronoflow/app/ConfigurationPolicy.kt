package net.chronoflow.app

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import net.chronoflow.core.datastore.DeviceConfigFile
import net.chronoflow.core.model.config.ConfigurationRules
import net.chronoflow.core.model.config.DeviceConfig
import net.chronoflow.core.model.config.LanguageChoice
import net.chronoflow.core.model.config.ThemeChoice
import java.util.Locale

/** Caps the font scale, coerces the locale to en or nl and forces left-to-right, whatever the phone says. */
object ConfigurationPolicy {
    data class LaunchConfiguration(
        val configuration: Configuration,
        val initialTheme: ThemeChoice,
    )

    fun overrideConfiguration(
        base: Context,
        sdkInt: Int = Build.VERSION.SDK_INT,
    ): Configuration = forLaunch(base, sdkInt).configuration

    fun forLaunch(
        base: Context,
        sdkInt: Int = Build.VERSION.SDK_INT,
    ): LaunchConfiguration {
        val current = base.resources.configuration
        val device = if (sdkInt >= 33) DeviceConfig() else DeviceConfigFile.readForLaunch(base, sdkInt)
        // From API 33 LocaleManager already put an explicit app language into the configuration.
        val choice = if (sdkInt >= 33) LanguageChoice.System else device.language
        val configuration =
            Configuration().apply {
                fontScale = ConfigurationRules.capFontScale(current.fontScale)
                setLocales(LocaleList(ConfigurationRules.resolveLocale(choice, current.locales[0])))
                // After setLocales, which also derives a layout direction.
                setLayoutDirection(Locale.ENGLISH)
            }
        return LaunchConfiguration(configuration, if (sdkInt < 31) device.theme else ThemeChoice.System)
    }

    /** For non-activity contexts; activities use applyOverrideConfiguration in attachBaseContext. */
    fun wrap(context: Context): Context = context.createConfigurationContext(overrideConfiguration(context))
}
