package net.chronoflow.feature.settings

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.model.session.StaleSessionException
import net.chronoflow.core.model.store.AccountRecord
import net.chronoflow.core.model.store.LocalMutationFailure
import net.chronoflow.core.model.store.SettingsRecord

data class SettingsPagesState(
    val account: AccountRecord? = null,
    val settings: List<SettingsRecord> = emptyList(),
    val loaded: Boolean = false,
    val settingsLoaded: Boolean = false,
    val failed: Boolean = false,
)

class SettingsPagesModel(
    private val deps: SettingsDependencies,
    private val scope: CoroutineScope,
) {
    private val mutableUi = MutableStateFlow(SettingsPagesState())
    val ui = mutableUi.asStateFlow()
    private val tasks = mutableListOf<Job>()
    private var active = true

    fun start() {
        if (!active || tasks.isNotEmpty()) return
        tasks +=
            scope.launch {
                settingsAttempt({ failed() }) {
                    deps.account().collect { account ->
                        if (active) mutableUi.update { it.copy(account = account, loaded = true) }
                    }
                }
            }
        tasks +=
            scope.launch {
                settingsAttempt({ failed() }) {
                    deps.settings().collect { settings ->
                        if (active) mutableUi.update { it.copy(settings = settings, settingsLoaded = true) }
                    }
                }
            }
        tasks +=
            scope.launch {
                settingsAttempt({ failed() }) {
                    deps.sessionUpdates().collect {
                        if (it.epoch != deps.snapshot.epoch ||
                            it.presence != SessionPresence.SignedIn(deps.snapshot.session.accountId)
                        ) {
                            stop()
                        }
                    }
                }
            }
    }

    suspend fun retry() {
        if (!active) return
        val previous = tasks.toList()
        tasks.clear()
        previous.forEach(Job::cancel)
        withContext(NonCancellable) { previous.joinAll() }
        currentCoroutineContext().ensureActive()
        if (!active) return
        mutableUi.update { it.copy(failed = false) }
        start()
        settingsAttempt({ failed() }) { deps.requestSync() }
    }

    suspend fun saveProfile(name: String) {
        if (!active || !deps.isCurrent()) throw StaleSessionException()
        currentCoroutineContext().ensureActive()
        val account = ui.value.account ?: throw LocalMutationFailure.MissingAccount
        deps.enqueueProfile(name, account.email)
    }

    suspend fun enqueueSettings(
        section: SettingsSection,
        patch: JsonObject,
    ) {
        if (!active || !deps.isCurrent()) throw StaleSessionException()
        currentCoroutineContext().ensureActive()
        deps.enqueueSettings(section, patch)
    }

    private fun failed() {
        if (active) mutableUi.update { it.copy(failed = true) }
    }

    fun stop() {
        active = false
        tasks.forEach(Job::cancel)
        tasks.clear()
        mutableUi.value = SettingsPagesState()
    }
}

internal fun SettingsPagesState.section(name: String): JsonObject? {
    val value = settings.firstOrNull { it.section == name }?.value ?: return null
    return try {
        ApiJson.response.parseToJsonElement(value) as? JsonObject
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}
