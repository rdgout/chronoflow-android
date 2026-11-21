package net.chronoflow.app.settings

import android.util.Log
import net.chronoflow.core.datastore.DeviceAppearance
import net.chronoflow.core.model.config.ThemeChoice
import net.chronoflow.feature.settings.preferences.ThemeDependencies

internal class AppThemeDependencies(
    private val appearance: DeviceAppearance,
) : ThemeDependencies {
    override val theme get() = appearance.theme

    override suspend fun setTheme(choice: ThemeChoice) = appearance.setTheme(choice)

    override fun reportThemeReadFailure() {
        Log.w("ChronoFlow", "Unable to read the device theme; using system appearance")
    }
}
