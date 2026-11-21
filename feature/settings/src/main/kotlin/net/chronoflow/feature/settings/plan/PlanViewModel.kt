package net.chronoflow.feature.settings.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.sync.SyncEvent
import net.chronoflow.core.sync.SyncState
import net.chronoflow.feature.settings.SettingsComputeKey
import net.chronoflow.feature.settings.SettingsDependencies
import net.chronoflow.feature.settings.SettingsDependenciesKey
import net.chronoflow.feature.settings.SettingsMainKey
import net.chronoflow.feature.settings.SettingsPagesModel
import net.chronoflow.feature.settings.section
import net.chronoflow.feature.settings.settingsAttempt

class PlanViewModel(
    private val deps: SettingsDependencies,
    main: CoroutineDispatcher,
    private val compute: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel(CoroutineScope(SupervisorJob() + main)) {
    private val pages = SettingsPagesModel(deps, viewModelScope)
    private val offline = MutableStateFlow(false)
    private val language = MutableStateFlow<Language?>(null)
    private val refreshing = MutableStateFlow(false)
    val ui =
        combine(pages.ui, offline, language, refreshing) { pages, offline, language, refreshing ->
            val plan =
                SettingsPlanState(
                    pages.section("planCredits"),
                    pages.section("billing"),
                    pages.settingsLoaded,
                    offline,
                )
            var line: SettingsPlanLine? = null
            if (language != null && plan.unlimited && plan.date != null) {
                settingsAttempt(failed = {}, cancelled = {}) {
                    line = withContext(compute) { SettingsPlanText.line(plan, deps.formatter(), language, deps.zone) }
                }
            }
            PlanScreenState(plan, line, refreshing)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, PlanScreenState())

    init {
        pages.start()
        viewModelScope.launch {
            settingsAttempt(failed = {}, cancelled = {}) {
                deps.syncEvents().collect { event ->
                    if (event is SyncEvent.Finished && event.epoch == deps.snapshot.epoch &&
                        event.accountID.rawValue == deps.snapshot.session.accountId
                    ) {
                        offline.value = event.result.state == SyncState.Offline
                    }
                }
            }
        }
    }

    fun setLanguage(language: Language) {
        this.language.value = language
    }

    fun refresh() {
        if (refreshing.value) return
        refreshing.value = true
        viewModelScope.launch {
            try {
                settingsAttempt(failed = {}, cancelled = {}) { deps.refreshNow() }
            } finally {
                refreshing.value = false
            }
        }
    }

    override fun onCleared() {
        pages.stop()
    }

    companion object {
        val Factory =
            viewModelFactory {
                initializer {
                    PlanViewModel(
                        checkNotNull(this[SettingsDependenciesKey]),
                        this[SettingsMainKey] ?: Dispatchers.Main.immediate,
                        this[SettingsComputeKey] ?: Dispatchers.Default,
                    )
                }
            }
    }
}
