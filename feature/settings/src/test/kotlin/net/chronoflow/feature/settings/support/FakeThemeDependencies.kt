package net.chronoflow.feature.settings.support

import kotlinx.coroutines.flow.MutableStateFlow
import net.chronoflow.core.model.config.ThemeChoice
import net.chronoflow.feature.settings.preferences.ThemeDependencies

class FakeThemeDependencies : ThemeDependencies {
    override val theme = MutableStateFlow(ThemeChoice.System)
    val choices = mutableListOf<ThemeChoice>()
    var failure: Throwable? = null
    var readFailures = 0

    override fun reportThemeReadFailure() {
        readFailures++
    }

    override suspend fun setTheme(choice: ThemeChoice) {
        failure?.let { throw it }
        choices += choice
        theme.value = choice
    }
}
