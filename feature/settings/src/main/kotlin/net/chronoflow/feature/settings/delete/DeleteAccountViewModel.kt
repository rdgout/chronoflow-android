package net.chronoflow.feature.settings.delete

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.api.AccountSecurityResult
import net.chronoflow.core.model.session.StaleSessionException
import net.chronoflow.feature.settings.SettingsDependencies
import net.chronoflow.feature.settings.SettingsDependenciesKey
import net.chronoflow.feature.settings.SettingsMainKey
import net.chronoflow.feature.settings.SettingsPagesModel
import net.chronoflow.feature.settings.security.SecurityState
import net.chronoflow.feature.settings.security.SettingsSecurityModel
import net.chronoflow.feature.settings.settingsAttempt

data class DeleteAccountUiState(
    val delete: DeleteState = DeleteState(),
    val reset: SecurityState = SecurityState(),
    val email: String = "",
) {
    val busy get() = delete.busy || reset.busy
    val retryAt get() = listOfNotNull(delete.retryAt, reset.retryAt).maxOrNull()
}

class DeleteAccountViewModel(
    private val deps: SettingsDependencies,
    main: CoroutineDispatcher,
) : ViewModel(CoroutineScope(SupervisorJob() + main)) {
    private val pages = SettingsPagesModel(deps, viewModelScope)
    private val model =
        SettingsDeleteModel({ password ->
            settingsAttempt(
                failed = { if (it is DataFailure) throw it else throw DataFailure.Server(null) },
                cancelled = { if (it is StaleSessionException) throw it else throw DataFailure.Server(null) },
            ) { deps.deleteAccount(password) }
        }, { deps.clock.now })
    private val reset =
        SettingsSecurityModel({ change ->
            var result: AccountSecurityResult? = null
            settingsAttempt(
                failed = { if (it is DataFailure) throw it else throw DataFailure.Server(null) },
                cancelled = { if (it is StaleSessionException) throw it else throw DataFailure.Server(null) },
            ) { result = deps.performSecurity(change) }
            checkNotNull(result)
        }, { deps.clock.now })
    val now = { deps.clock.now }
    val ui =
        combine(pages.ui, model.state, reset.state) { pages, delete, reset ->
            DeleteAccountUiState(delete, reset, pages.account?.email.orEmpty())
        }.stateIn(viewModelScope, SharingStarted.Eagerly, DeleteAccountUiState())

    init {
        pages.start()
    }

    fun event(event: DeleteAccountEvent) {
        if (model.state.value.busy || reset.state.value.busy) return
        when (event) {
            DeleteAccountEvent.Confirm -> model.confirmDeletion()
            is DeleteAccountEvent.Submit -> viewModelScope.launch { model.submit(event.password) }
            DeleteAccountEvent.Reset -> viewModelScope.launch { reset.sendReset(ui.value.email) }
            DeleteAccountEvent.Dismiss -> stop()
        }
    }

    fun stop() {
        model.stop()
        reset.clearSecrets()
    }

    override fun onCleared() {
        stop()
        pages.stop()
    }

    companion object {
        val Factory =
            viewModelFactory {
                initializer {
                    DeleteAccountViewModel(
                        checkNotNull(this[SettingsDependenciesKey]),
                        this[SettingsMainKey] ?: Dispatchers.Main.immediate,
                    )
                }
            }
    }
}
