package net.chronoflow.feature.reminders

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.chronoflow.core.reminders.ReminderPermission
import net.chronoflow.core.reminders.ReminderSettings

@Immutable
data class RemindersUiState(
    val settings: ReminderSettings? = null,
    val permission: ReminderPermission? = null,
    val busy: Boolean = false,
) {
    val active: Boolean get() = permission?.remindersActive(settings) == true
}

/** Port of iOS `RemindersModel`. */
class RemindersViewModel(
    private val deps: RemindersDependencies,
    main: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel(CoroutineScope(SupervisorJob() + main)) {
    private val mutableState = MutableStateFlow(RemindersUiState())
    val state = mutableState.asStateFlow()
    private var failure: Throwable? = null
    val failed: Boolean get() = failure != null
    private var observation: Job? = null
    private var sequence = 0L
    private val launches = MutableStateFlow<Long?>(null)
    val permissionLaunches = launches.asStateFlow()
    private val completions = MutableStateFlow<Long?>(null)
    val permissionRequestsFinished = completions.asStateFlow()
    private var awaitingResult = false

    fun acknowledgePermissionLaunch(id: Long) {
        launches.compareAndSet(id, null)
    }

    fun acknowledgePermissionFinished(id: Long) {
        completions.compareAndSet(id, null)
    }

    fun start() {
        if (observation?.isActive == true) return
        observation =
            viewModelScope.launch {
                remindersAttempt({ failure = it }) {
                    deps.reminderSettings().collect { value -> mutableState.update { it.copy(settings = value) } }
                }
            }
        refreshPermission()
    }

    fun stop() {
        observation?.cancel()
        observation = null
    }

    fun refreshPermission() =
        viewModelScope.launch {
            remindersAttempt({ failure = it }) {
                val previous = state.value.permission
                val permission = deps.permission()
                mutableState.update { it.copy(permission = permission) }
                if (permission == ReminderPermission.Granted &&
                    (previous == ReminderPermission.Blocked || previous == ReminderPermission.Undetermined) &&
                    !state.value.busy
                ) {
                    write(true)
                }
            }
        }

    fun requestPermission() {
        if (state.value.busy) return
        mutableState.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                remindersAttempt({ failure = it }) {
                    when (deps.beginPermissionRequest()) {
                        PermissionRequestStart.SystemDialog -> {
                            awaitingResult = true
                            launches.value = ++sequence
                        }

                        PermissionRequestStart.NoDialog -> {
                            finishPermissionRequest()
                        }
                    }
                }
            } finally {
                if (!awaitingResult) mutableState.update { it.copy(busy = false) }
            }
        }
    }

    fun onPermissionResult() {
        if (!awaitingResult) return
        awaitingResult = false
        launches.value = null
        viewModelScope.launch { finishPermissionRequest() }
    }

    private suspend fun finishPermissionRequest() {
        try {
            remindersAttempt({ failure = it }) {
                val permission = deps.permission()
                mutableState.update { it.copy(permission = permission) }
                if (permission == ReminderPermission.Granted) write(true)
                deps.reschedule()
            }
        } finally {
            mutableState.update { it.copy(busy = false) }
            completions.value = ++sequence
        }
    }

    fun setEnabled(enabled: Boolean) {
        if (state.value.busy || state.value.permission != ReminderPermission.Granted) return
        mutate { write(enabled) }
    }

    fun setLead(minutes: Int) {
        val patch = ReminderSettings.leadPatch(minutes) ?: return
        if (state.value.busy || state.value.settings == null) return
        mutate {
            deps.enqueueReminders(patch)
            deps.reschedule()
        }
    }

    private fun mutate(block: suspend () -> Unit) {
        mutableState.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                remindersAttempt({ failure = it }, block)
            } finally {
                mutableState.update { it.copy(busy = false) }
            }
        }
    }

    private suspend fun write(enabled: Boolean) {
        if (state.value.settings == null) return
        deps.enqueueReminders(ReminderSettings.enabledPatch(enabled))
        failure = null
        deps.reschedule()
    }

    companion object {
        val Factory =
            viewModelFactory {
                initializer {
                    RemindersViewModel(
                        checkNotNull(this[RemindersDependenciesKey]),
                        this[RemindersMainKey] ?: Dispatchers.Main.immediate,
                    )
                }
            }
    }
}
