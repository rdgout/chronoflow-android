package net.chronoflow.feature.settings.language

import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import net.chronoflow.core.model.config.LanguageChoice

interface LanguageDependencies {
    val current: Flow<LanguageChoice>

    fun phoneLanguage(): String

    suspend fun apply(choice: LanguageChoice): Boolean

    val feedback: MutableStateFlow<LanguageChoice?>
}

object LanguageDependenciesKey : CreationExtras.Key<LanguageDependencies>
