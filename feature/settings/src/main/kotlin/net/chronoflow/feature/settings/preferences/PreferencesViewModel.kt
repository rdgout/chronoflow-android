package net.chronoflow.feature.settings.preferences

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.model.config.ThemeChoice
import net.chronoflow.feature.settings.SettingsDependencies
import net.chronoflow.feature.settings.SettingsDependenciesKey
import net.chronoflow.feature.settings.SettingsMainKey
import net.chronoflow.feature.settings.SettingsPagesModel
import net.chronoflow.feature.settings.SettingsPagesState
import net.chronoflow.feature.settings.section
import net.chronoflow.feature.settings.settingsAttempt

@Immutable
data class PreferencesUiState(
    val pages: SettingsPagesState = SettingsPagesState(),
    val editor: SettingsPreferencesModel.State? = null,
    val theme: ThemeChoice = ThemeChoice.System,
    val themeFailure: Boolean = false,
) {
    val failure: Boolean get() = themeFailure || editor?.failure == true
}

class PreferencesViewModel(
    deps: SettingsDependencies,
    private val theme: ThemeDependencies,
    main: CoroutineDispatcher,
) : ViewModel(CoroutineScope(SupervisorJob() + main)) {
    private val pages = SettingsPagesModel(deps, viewModelScope)
    private var model: SettingsPreferencesModel? = null
    private var sectionAvailable = false
    private val mutableUi = MutableStateFlow(PreferencesUiState())
    val ui = mutableUi.asStateFlow()

    init {
        pages.start()
        viewModelScope.launch {
            pages.ui.collect { page ->
                val section = page.section("preferences")
                sectionAvailable = section != null
                if (section != null) {
                    val editor = model
                    if (editor == null) {
                        val created =
                            SettingsPreferencesModel(section) { patch ->
                                pages.enqueueSettings(SettingsSection.Preferences, patch)
                            }
                        model = created
                        viewModelScope.launch {
                            created.state.collect { state ->
                                mutableUi.update { it.copy(editor = state.takeIf { sectionAvailable }) }
                            }
                        }
                    } else {
                        editor.receive(section)
                    }
                }
                mutableUi.update { it.copy(pages = page, editor = model?.state?.value.takeIf { sectionAvailable }) }
            }
        }
        viewModelScope.launch {
            settingsAttempt(failed = {
                theme.reportThemeReadFailure()
                mutableUi.update { it.copy(theme = ThemeChoice.System) }
            }) {
                theme.theme.collect { choice -> mutableUi.update { it.copy(theme = choice) } }
            }
        }
    }

    fun setTimeFormat(format: String) {
        viewModelScope.launch { model?.setTimeFormat(format) }
    }

    fun setShown(
        shown: Boolean,
        group: SettingsTimelineGroup,
    ) {
        viewModelScope.launch { model?.setShown(shown, group) }
    }

    fun setTheme(id: String) {
        val choice = ThemeChoice.fromRaw(id) ?: return
        viewModelScope.launch {
            settingsAttempt(failed = { mutableUi.update { it.copy(themeFailure = true) } }, cancelled = {}) {
                theme.setTheme(choice)
                mutableUi.update { it.copy(themeFailure = false) }
            }
        }
    }

    fun retry() {
        viewModelScope.launch { pages.retry() }
    }

    override fun onCleared() {
        pages.stop()
    }

    companion object {
        val Factory =
            viewModelFactory {
                initializer {
                    PreferencesViewModel(
                        checkNotNull(this[SettingsDependenciesKey]),
                        checkNotNull(this[ThemeDependenciesKey]),
                        this[SettingsMainKey] ?: Dispatchers.Main.immediate,
                    )
                }
            }
    }
}
