package net.chronoflow.feature.settings.language

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
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.chronoflow.core.model.config.LanguageChoice
import net.chronoflow.feature.settings.SettingsMainKey
import net.chronoflow.feature.settings.settingsAttempt

data class LanguageNotice(
    val id: Long,
    val choice: LanguageChoice,
)

data class LanguageUiState(
    val choice: LanguageChoice? = null,
    val phone: String = "en",
    val busy: Boolean = false,
    val recreate: Long? = null,
    val notice: LanguageNotice? = null,
)

class LanguageSettingsViewModel(
    private val deps: LanguageDependencies,
    main: CoroutineDispatcher,
) : ViewModel(CoroutineScope(SupervisorJob() + main)) {
    private val mutableUi = MutableStateFlow(LanguageUiState())
    val ui = mutableUi.asStateFlow()
    private var nextId = 0L
    internal var failure: Throwable? = null
        private set

    init {
        viewModelScope.launch {
            settingsAttempt({ failure = it }) {
                deps.current.collect { choice ->
                    mutableUi.update { it.copy(choice = choice, phone = deps.phoneLanguage()) }
                }
            }
        }
    }

    fun select(choice: LanguageChoice) {
        if (ui.value.choice == null || ui.value.choice == choice || ui.value.busy) return
        mutableUi.update { it.copy(busy = true) }
        viewModelScope.launch {
            var applied = false
            deps.feedback.value = choice
            try {
                settingsAttempt({ failure = it }) {
                    val recreate = deps.apply(choice)
                    applied = true
                    failure = null
                    mutableUi.update { it.copy(choice = choice, recreate = if (recreate) ++nextId else null) }
                }
            } finally {
                if (!applied) deps.feedback.compareAndSet(choice, null)
                mutableUi.update { it.copy(busy = false) }
            }
        }
    }

    fun arrived() {
        deps.feedback.getAndUpdate { null }?.let { choice ->
            mutableUi.update { it.copy(notice = LanguageNotice(++nextId, choice)) }
        }
    }

    fun recreated(id: Long) {
        mutableUi.update { if (it.recreate == id) it.copy(recreate = null) else it }
    }

    fun presented(id: Long) {
        mutableUi.update { if (it.notice?.id == id) it.copy(notice = null) else it }
    }

    companion object {
        val Factory =
            viewModelFactory {
                initializer {
                    LanguageSettingsViewModel(
                        checkNotNull(this[LanguageDependenciesKey]),
                        this[SettingsMainKey] ?: Dispatchers.Main.immediate,
                    )
                }
            }
    }
}
