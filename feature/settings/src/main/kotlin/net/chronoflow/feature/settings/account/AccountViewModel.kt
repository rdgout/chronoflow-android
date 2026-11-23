package net.chronoflow.feature.settings.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.chronoflow.feature.settings.SettingsDependencies
import net.chronoflow.feature.settings.SettingsDependenciesKey
import net.chronoflow.feature.settings.SettingsMainKey
import net.chronoflow.feature.settings.SettingsPagesModel

class AccountViewModel(
    deps: SettingsDependencies,
    main: CoroutineDispatcher,
) : ViewModel(
        CoroutineScope(SupervisorJob() + main),
    ) {
    private val model = SettingsPagesModel(deps, viewModelScope)
    val ui = model.ui

    init {
        model.start()
    }

    fun retry() {
        viewModelScope.launch { model.retry() }
    }

    override fun onCleared() {
        model.stop()
    }

    companion object {
        val Factory =
            viewModelFactory {
                initializer {
                    AccountViewModel(
                        checkNotNull(this[SettingsDependenciesKey]),
                        this[SettingsMainKey] ?: Dispatchers.Main.immediate,
                    )
                }
            }
    }
}
