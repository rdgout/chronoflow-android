package net.chronoflow.feature.settings.home

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

class SettingsHomeViewModel(
    deps: SettingsDependencies,
    main: CoroutineDispatcher,
) : ViewModel(CoroutineScope(SupervisorJob() + main)) {
    private val model = SettingsHomeModel(deps, viewModelScope)
    val ui = model.ui
    internal val failure get() = model.failure

    init {
        model.start()
    }

    fun stay() = model.stay()

    fun signOut(discardPending: Boolean = false) {
        viewModelScope.launch { model.signOut(discardPending) }
    }

    override fun onCleared() {
        model.stop()
    }

    companion object {
        val Factory =
            viewModelFactory {
                initializer {
                    SettingsHomeViewModel(
                        checkNotNull(this[SettingsDependenciesKey]),
                        this[SettingsMainKey] ?: Dispatchers.Main.immediate,
                    )
                }
            }
    }
}
