package net.chronoflow.feature.settings.preferences

import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.flow.Flow
import net.chronoflow.core.model.config.ThemeChoice

interface ThemeDependencies {
    val theme: Flow<ThemeChoice>

    suspend fun setTheme(choice: ThemeChoice)
}

object ThemeDependenciesKey : CreationExtras.Key<ThemeDependencies>
