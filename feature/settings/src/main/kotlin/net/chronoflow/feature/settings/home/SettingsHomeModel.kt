package net.chronoflow.feature.settings.home

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.model.session.SignOutResult
import net.chronoflow.core.model.store.OutboxRecord
import net.chronoflow.core.sync.SyncEvent
import net.chronoflow.core.sync.SyncState
import net.chronoflow.feature.settings.SettingsDependencies
import net.chronoflow.feature.settings.settingsAttempt

data class SettingsHomeUi(
    val state: SettingsHomeState = SettingsHomeState(),
    val syncing: Boolean = false,
    val offline: Boolean = false,
    val busy: Boolean = false,
    val showsPending: Boolean = false,
    val confirmationCount: Int = 0,
)

class SettingsHomeModel(
    private val deps: SettingsDependencies,
    private val scope: CoroutineScope,
) {
    private val mutableUi = MutableStateFlow(SettingsHomeUi())
    val ui = mutableUi.asStateFlow()
    internal var failure: Throwable? = null
        private set
    private var active = true
    private var generation = 0L
    private val tasks = mutableListOf<Job>()

    fun start() {
        if (!active || tasks.isNotEmpty()) return
        observe(deps.account()) { account -> mutableUi.update { it.copy(state = it.state.copy(account = account)) } }
        observe(
            deps.settings(),
        ) { settings -> mutableUi.update { it.copy(state = it.state.apply(settings = settings)) } }
        observe(deps.outbox(), ::receive)
        observe(deps.sessionUpdates()) { update ->
            if (update.epoch != deps.snapshot.epoch ||
                update.presence != SessionPresence.SignedIn(deps.snapshot.session.accountId)
            ) {
                stop()
            }
        }
        observe(deps.syncEvents()) { event ->
            when (event) {
                is SyncEvent.Started -> {
                    if (event.accountID.rawValue == deps.snapshot.session.accountId &&
                        event.epoch == deps.snapshot.epoch
                    ) {
                        mutableUi.update { it.copy(syncing = true) }
                    }
                }

                is SyncEvent.Finished -> {
                    if (event.accountID.rawValue == deps.snapshot.session.accountId &&
                        event.epoch == deps.snapshot.epoch
                    ) {
                        mutableUi.update {
                            it.copy(syncing = false, offline = event.result.state == SyncState.Offline)
                        }
                    }
                }
            }
        }
    }

    private fun <T> observe(
        flow: Flow<T>,
        receive: (T) -> Unit,
    ) {
        tasks +=
            scope.launch {
                settingsAttempt({ if (active) failure = it }) { flow.collect { if (active) receive(it) } }
            }
    }

    private fun receive(outbox: List<OutboxRecord>) {
        mutableUi.update {
            val state = it.state.apply(outbox = outbox)
            it.copy(
                state = state,
                confirmationCount = state.pending.size,
                showsPending = it.showsPending && (outbox.isNotEmpty() || it.busy),
            )
        }
    }

    fun stay() {
        if (!ui.value.busy) mutableUi.update { it.copy(showsPending = false) }
    }

    suspend fun signOut(discardPending: Boolean = false) {
        if (!active || ui.value.busy || (discardPending && !ui.value.showsPending)) return
        mutableUi.update { it.copy(busy = true) }
        val generation = generation
        try {
            settingsAttempt({ if (active && this.generation == generation) failure = it }) {
                if (!current(generation)) return@settingsAttempt
                val result = deps.signOut(discardPending)
                if (!active || this.generation != generation) return@settingsAttempt
                when (result) {
                    SignOutResult.SignedOut -> {
                        stop()
                    }

                    is SignOutResult.NeedsConfirmation -> {
                        if (!current(generation)) return@settingsAttempt
                        mutableUi.update { it.copy(confirmationCount = result.pendingCount) }
                        val pending = deps.pendingOutbox()
                        if (!current(generation)) return@settingsAttempt
                        receive(pending)
                        if (!current(generation)) return@settingsAttempt
                        mutableUi.update { it.copy(showsPending = it.confirmationCount > 0) }
                    }
                }
                failure = null
            }
        } finally {
            mutableUi.update {
                it.copy(
                    busy = false,
                    showsPending =
                        it.showsPending &&
                            !(it.state.outboxLoaded && it.state.pendingCount == 0),
                )
            }
        }
    }

    private suspend fun current(generation: Long): Boolean = deps.isCurrent() && active && this.generation == generation

    fun stop() {
        active = false
        generation++
        tasks.forEach(Job::cancel)
        tasks.clear()
        mutableUi.value = SettingsHomeUi()
    }
}
