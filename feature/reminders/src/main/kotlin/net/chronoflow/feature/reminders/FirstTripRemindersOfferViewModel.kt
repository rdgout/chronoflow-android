package net.chronoflow.feature.reminders

import android.util.Log
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FirstTripRemindersOfferViewModel(
    private val deps: RemindersDependencies,
    main: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel(CoroutineScope(SupervisorJob() + main)) {
    private var started = false
    private val mutableShowing = MutableStateFlow(false)
    val showing = mutableShowing.asStateFlow()

    fun start() {
        if (started) return
        started = true
        viewModelScope.launch {
            remindersAttempt({ Log.w("CfReminders", "First-trip offer unavailable: ${it.javaClass.simpleName}") }) {
                deps.settingsChanges().first {
                    if (!deps.isCurrent()) return@first true
                    val permission = deps.permission()
                    val result = deps.consumeFirstTripOffer(permission) ?: return@first false
                    if (deps.isCurrent()) mutableShowing.value = result
                    true
                }
            }
        }
    }

    fun dismiss() {
        mutableShowing.value = false
    }

    companion object {
        val Factory =
            viewModelFactory {
                initializer {
                    FirstTripRemindersOfferViewModel(
                        checkNotNull(this[RemindersDependenciesKey]),
                        this[RemindersMainKey] ?: Dispatchers.Main.immediate,
                    )
                }
            }
    }
}
