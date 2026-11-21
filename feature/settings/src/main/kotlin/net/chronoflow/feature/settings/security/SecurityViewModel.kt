package net.chronoflow.feature.settings.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.zxing.WriterException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.api.AccountSecurityChange
import net.chronoflow.core.model.api.AccountSecurityResult
import net.chronoflow.core.model.session.StaleSessionException
import net.chronoflow.feature.settings.SettingsComputeKey
import net.chronoflow.feature.settings.SettingsDependencies
import net.chronoflow.feature.settings.SettingsDependenciesKey
import net.chronoflow.feature.settings.SettingsIoKey
import net.chronoflow.feature.settings.SettingsMainKey
import net.chronoflow.feature.settings.SettingsPagesModel
import net.chronoflow.feature.settings.SettingsPagesState
import net.chronoflow.feature.settings.section
import net.chronoflow.feature.settings.settingsAttempt
import java.time.Instant

data class SecurityFeedback(
    val id: Long,
    val key: String,
)

data class SecurityUiState(
    val pages: SettingsPagesState = SettingsPagesState(),
    val summary: SecuritySummary? = null,
    val security: SecurityState = SecurityState(),
    val feedback: SecurityFeedback? = null,
)

class SecurityViewModel(
    private val deps: SettingsDependencies,
    main: CoroutineDispatcher,
    private val compute: CoroutineDispatcher = Dispatchers.Default,
    internal val io: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel(CoroutineScope(SupervisorJob() + main)) {
    private val pages = SettingsPagesModel(deps, viewModelScope)
    internal val model = SettingsSecurityModel(::perform) { deps.clock.now }
    val state = model.state
    val now: () -> Instant = { deps.clock.now }
    private val acknowledged = MutableStateFlow(0L)
    private var request: Job? = null
    private val mutableQr = MutableStateFlow<QrMatrix?>(null)
    val qr: kotlinx.coroutines.flow.StateFlow<QrMatrix?> = mutableQr
    private var seeded = false
    val summary =
        pages.ui
            .map { SecuritySummary.from(it.section("security")) }
            .flowOn(compute)
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val email =
        pages.ui
            .map { it.account?.email.orEmpty() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val ui =
        combine(pages.ui, summary, state, acknowledged) { page, summary, state, acknowledged ->
            SecurityUiState(
                page,
                summary,
                state,
                if (state.feedbackRevision > acknowledged && state.message != null && state.retryAt == null &&
                    !state.twoFactorActivationFailed && state.prompt == null
                ) {
                    SecurityFeedback(state.feedbackRevision, state.message)
                } else {
                    null
                },
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, SecurityUiState())

    init {
        pages.start()
        viewModelScope.launch {
            state.map { it.setup }.distinctUntilChanged().collectLatest { setup ->
                mutableQr.value = null
                if (setup != null) {
                    val matrix =
                        try {
                            withContext(compute) { QrMatrix.encode(setup.otpauthUrl) }
                        } catch (_: WriterException) {
                            null
                        } catch (_: IllegalArgumentException) {
                            null
                        }
                    if (state.value.setup == setup) mutableQr.value = matrix
                }
            }
        }
        viewModelScope.launch {
            state.map { it.retryAt }.distinctUntilChanged().collect { retryAt ->
                if (retryAt != null) {
                    while (model.locked) delay(1_000)
                    model.refreshLock()
                }
            }
        }
    }

    private suspend fun perform(change: AccountSecurityChange): AccountSecurityResult {
        var result: AccountSecurityResult? = null
        settingsAttempt(
            failed = { failure ->
                if (failure is DataFailure) throw failure
                throw DataFailure.Server(null)
            },
            cancelled = { failure ->
                if (failure is StaleSessionException) throw failure
                throw DataFailure.Server(null)
            },
        ) { result = deps.performSecurity(change) }
        return checkNotNull(result)
    }

    private fun submit(action: suspend () -> Unit) {
        if (request?.isActive == true) return
        request = viewModelScope.launch { action() }
    }

    fun changePassword(
        current: SecretField,
        new: SecretField,
        confirmation: SecretField,
    ) = submit { model.changePassword(current, new, confirmation) }

    fun showPrompt(prompt: SettingsSecurityPrompt) = model.showPrompt(prompt)

    fun cancelPrompt() = model.cancelPrompt()

    fun submitPrompt(password: SecretField) = submit { model.submitPrompt(password) }

    fun confirm(code: SecretField) = submit { model.confirm(code) }

    fun sendReset() = submit { model.sendReset(email.value) }

    fun retry() = submit { pages.retry() }

    fun feedbackPresented(id: Long) {
        if (state.value.feedbackRevision == id) acknowledged.value = id
    }

    fun startTwoFactor() {
        if (seeded) return
        seeded = true
        deps.passwordPromptSeed?.let { password ->
            showPrompt(SettingsSecurityPrompt.Enable)
            submitPrompt(
                object : SecretField {
                    override fun read() = password

                    override fun clear() = Unit

                    override fun toString() = "SecretField(***)"
                },
            )
        }
    }

    fun copySecret() {
        state.value.setup
            ?.secretKey
            ?.let(deps.clipboard::copy)
    }

    fun copyCodes() {
        if (state.value.codes.isNotEmpty()) deps.clipboard.copy(state.value.codes.joinToString("\n"))
    }

    fun download(
        uri: android.net.Uri,
        resolver: android.content.ContentResolver,
    ) {
        viewModelScope.launch {
            settingsAttempt(failed = { model.message("Something went wrong") }, cancelled = {}) {
                withContext(io) { writeRecoveryCodes(resolver, uri) { state.value.codes } }
            }
        }
    }

    suspend fun confirmedDate(language: net.chronoflow.core.model.contracts.Language): String? {
        val raw = summary.value?.twoFactorConfirmedAt ?: return null
        try {
            Instant.parse(raw)
        } catch (_: java.time.format.DateTimeParseException) {
            return null
        }
        var result: String? = null
        settingsAttempt(failed = { model.message("Something went wrong") }, cancelled = {}) {
            result = withContext(compute) { deps.formatter().formatDate(raw, language, deps.zone.id) }
        }
        return result
    }

    fun clearSecrets() {
        mutableQr.value = null
        model.clearSecrets()
    }

    override fun onCleared() {
        mutableQr.value = null
        model.clearSecrets()
        pages.stop()
    }

    companion object {
        val Factory =
            viewModelFactory {
                initializer {
                    SecurityViewModel(
                        checkNotNull(this[SettingsDependenciesKey]),
                        this[SettingsMainKey] ?: Dispatchers.Main.immediate,
                        this[SettingsComputeKey] ?: Dispatchers.Default,
                        this[SettingsIoKey] ?: Dispatchers.IO,
                    )
                }
            }
    }
}
