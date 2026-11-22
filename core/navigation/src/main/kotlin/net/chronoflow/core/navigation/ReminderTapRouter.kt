package net.chronoflow.core.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReminderTapRouter {
    private val mutablePending = MutableStateFlow<ReminderTapExtras?>(null)
    val pending = mutablePending.asStateFlow()

    fun receive(tap: ReminderTapExtras) {
        mutablePending.value = tap
    }

    fun clear() {
        mutablePending.value = null
    }
}
