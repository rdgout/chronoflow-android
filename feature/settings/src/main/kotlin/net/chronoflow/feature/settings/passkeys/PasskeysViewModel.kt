package net.chronoflow.feature.settings.passkeys

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.passkey.PasskeyRegistrationProviding
import net.chronoflow.core.model.session.StaleSessionException
import net.chronoflow.feature.settings.SettingsComputeKey
import net.chronoflow.feature.settings.SettingsDependencies
import net.chronoflow.feature.settings.SettingsDependenciesKey
import net.chronoflow.feature.settings.SettingsMainKey
import net.chronoflow.feature.settings.SettingsPagesModel
import net.chronoflow.feature.settings.security.SecretField
import net.chronoflow.feature.settings.security.SecurityFeedback
import net.chronoflow.feature.settings.security.SecurityUiState
import net.chronoflow.feature.settings.security.SettingsSecurityModel
import net.chronoflow.feature.settings.settingsAttempt

class PasskeysViewModel(
    private val deps: SettingsDependencies,
    main: CoroutineDispatcher,
    private val compute: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel(CoroutineScope(SupervisorJob() + main)) {
    val passkeysEnabled = deps.passkeyCapability.enabled
    private val pages = SettingsPagesModel(deps, viewModelScope)
    private val model =
        SettingsSecurityModel(
            { change -> attempt { deps.performSecurity(change) } },
            { deps.clock.now },
            deps.passkeyRegistration?.let { provider ->
                PasskeyRegistrationProviding { challenge -> attempt { provider.registration(challenge) } }
            },
            { attempt { deps.deviceName() } },
        )
    private val acknowledged = MutableStateFlow(0L)

    private data class PasskeyLocale(
        val language: Language,
        val text: (String, Map<String, String>) -> String,
    )

    private val locale = MutableStateFlow<PasskeyLocale?>(null)
    private val localDate =
        (if (passkeysEnabled) deps.clock.values() else emptyFlow())
            .map { instant ->
                val zone = deps.zone
                zone to instant.atZone(zone).toLocalDate()
            }.distinctUntilChanged()
            .flowOn(compute)
    val refreshing = MutableStateFlow(false)
    val canRegisterPasskey get() = model.canRegisterPasskey
    val now = { deps.clock.now }
    val ui =
        combine(pages.ui, model.state, acknowledged) { pages, security, acknowledged ->
            SecurityUiState(
                pages = pages,
                security = security,
                feedback =
                    if (security.feedbackRevision > acknowledged && security.message != null &&
                        security.retryAt == null && security.prompt == null
                    ) {
                        SecurityFeedback(security.feedbackRevision, security.message)
                    } else {
                        null
                    },
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, SecurityUiState())
    val meta =
        combine(
            model.state.map { it.passkeys }.distinctUntilChanged(),
            locale,
            localDate,
        ) { passkeys, locale, (zone, date) ->
            if (locale == null || passkeys.isNullOrEmpty()) {
                emptyMap()
            } else {
                withContext(compute) {
                    val formatter = deps.formatter()
                    val (language, text) = locale
                    val now = date.atStartOfDay(zone).toInstant()
                    passkeys
                        .mapNotNull { passkey ->
                            SettingsPasskeyText
                                .meta(passkey, language, now, zone, formatter, text)
                                ?.let { passkey.id to it }
                        }.toMap()
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000), emptyMap())

    init {
        if (passkeysEnabled) {
            pages.start()
            reload()
            viewModelScope.launch {
                model.state.map { it.retryAt }.distinctUntilChanged().collect { retryAt ->
                    if (retryAt != null) {
                        while (model.locked) delay(1_000)
                        model.refreshLock()
                    }
                }
            }
        }
    }

    private suspend fun <T> attempt(work: suspend () -> T): T {
        var result: T? = null
        settingsAttempt(
            failed = { if (it is DataFailure) throw it else throw DataFailure.Server(null) },
            cancelled = { if (it is StaleSessionException) throw it else throw DataFailure.Server(null) },
        ) { result = work() }
        return checkNotNull(result)
    }

    fun setLanguage(
        language: Language,
        text: (String, Map<String, String>) -> String,
    ) {
        locale.value = PasskeyLocale(language, text)
    }

    fun reload() {
        if (!passkeysEnabled || refreshing.value) return
        viewModelScope.launch {
            refreshing.value = true
            try {
                model.loadPasskeys()
            } finally {
                refreshing.value = false
            }
        }
    }

    fun resume() {
        if (model.state.value.offline) reload()
    }

    fun event(event: PasskeysEvent) {
        when (event) {
            PasskeysEvent.Refresh -> {
                reload()
            }

            PasskeysEvent.Add -> {
                model.showPrompt(
                    net.chronoflow.feature.settings.security.SettingsSecurityPrompt.AddPasskey,
                )
            }

            is PasskeysEvent.Remove -> {
                model.showPrompt(
                    net.chronoflow.feature.settings.security.SettingsSecurityPrompt.RemovePasskey,
                    event.passkey,
                )
            }

            PasskeysEvent.Cancel -> {
                model.cancelPrompt()
            }

            is PasskeysEvent.Submit -> {
                submit { model.submitPrompt(event.password) }
            }

            PasskeysEvent.Reset -> {
                submit {
                    model.sendReset(
                        ui.value.pages.account
                            ?.email
                            .orEmpty(),
                    )
                }
            }

            PasskeysEvent.Back -> {
                return
            }
        }
    }

    private fun submit(work: suspend () -> Unit) {
        if (model.state.value.busy) return
        viewModelScope.launch { work() }
    }

    fun feedbackPresented(id: Long) {
        if (model.state.value.feedbackRevision == id) acknowledged.value = id
    }

    fun clearSecrets() = model.clearSecrets()

    override fun onCleared() {
        model.clearSecrets()
        pages.stop()
    }

    companion object {
        val Factory =
            viewModelFactory {
                initializer {
                    PasskeysViewModel(
                        checkNotNull(this[SettingsDependenciesKey]),
                        this[SettingsMainKey] ?: Dispatchers.Main.immediate,
                        this[SettingsComputeKey] ?: Dispatchers.Default,
                    )
                }
            }
    }
}
