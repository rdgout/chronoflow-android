package net.chronoflow.app.reminders

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.model.config.ConfigurationRules
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.feature.settings.language.LanguageDependencies
import java.util.Locale

class AppLanguageSource(
    private val languages: LanguageDependencies,
    private val io: CoroutineDispatcher,
) {
    suspend fun current(): Language =
        withContext(io) {
            val choice = languages.current.first()
            when (ConfigurationRules.resolveLanguage(choice, Locale.forLanguageTag(languages.phoneLanguage()))) {
                AppLanguage.Nl -> Language.Nl
                AppLanguage.En -> Language.En
            }
        }
}
