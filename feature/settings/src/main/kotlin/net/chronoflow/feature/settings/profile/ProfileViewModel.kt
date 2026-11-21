package net.chronoflow.feature.settings.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
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
import net.chronoflow.feature.settings.SettingsDependencies
import net.chronoflow.feature.settings.SettingsDependenciesKey
import net.chronoflow.feature.settings.SettingsMainKey
import net.chronoflow.feature.settings.SettingsPagesModel
import net.chronoflow.feature.settings.SettingsPagesState

data class ProfileUiState(
    val pages: SettingsPagesState = SettingsPagesState(),
    val editor: SettingsProfileState? = null,
)

class ProfileViewModel(
    deps: SettingsDependencies,
    private val savedState: SavedStateHandle,
    main: CoroutineDispatcher,
) : ViewModel(CoroutineScope(SupervisorJob() + main)) {
    private val pages = SettingsPagesModel(deps, viewModelScope)
    private var model: SettingsProfileModel? = null
    private val mutableUi = MutableStateFlow(ProfileUiState())
    val ui = mutableUi.asStateFlow()

    init {
        pages.start()
        viewModelScope.launch {
            pages.ui.collect { page ->
                page.account?.let { account ->
                    val editor = model
                    if (editor == null) {
                        val created = SettingsProfileModel(account, savedState[NAME], pages::saveProfile)
                        model = created
                        viewModelScope.launch {
                            created.ui.collect { state ->
                                savedState[NAME] = state.name
                                mutableUi.update { it.copy(editor = state) }
                            }
                        }
                    } else {
                        editor.receive(account)
                    }
                }
                mutableUi.update { it.copy(pages = page) }
            }
        }
    }

    fun edit(name: String) {
        model?.edit(name)
        savedState[NAME] = name
    }

    fun submit() {
        viewModelScope.launch { model?.submit() }
    }

    fun retry() {
        viewModelScope.launch { pages.retry() }
    }

    override fun onCleared() {
        pages.stop()
    }

    companion object {
        private const val NAME = "settings.profile.name"
        val Factory =
            viewModelFactory {
                initializer {
                    ProfileViewModel(
                        checkNotNull(this[SettingsDependenciesKey]),
                        createSavedStateHandle(),
                        this[SettingsMainKey] ?: Dispatchers.Main.immediate,
                    )
                }
            }
    }
}
