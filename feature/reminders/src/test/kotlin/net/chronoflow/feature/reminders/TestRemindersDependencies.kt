package net.chronoflow.feature.reminders

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonObject
import net.chronoflow.core.reminders.ReminderPermission
import net.chronoflow.core.reminders.ReminderSettings

internal open class TestRemindersDependencies : RemindersDependencies {
    val settings = MutableStateFlow<ReminderSettings?>(ReminderSettings())
    var authorization = ReminderPermission.Granted
    var start = PermissionRequestStart.NoDialog
    val patches = mutableListOf<JsonObject>()
    var reschedules = 0
    var requests = 0
    var current = true
    var enqueue: suspend (JsonObject) -> Unit = {}
    var request: suspend () -> Unit = {}

    override fun reminderSettings(): kotlinx.coroutines.flow.Flow<ReminderSettings?> = settings

    override fun settingsChanges() = settings.map { }

    override suspend fun permission() = authorization

    override suspend fun beginPermissionRequest(): PermissionRequestStart {
        requests++
        request()
        return start
    }

    override suspend fun enqueueReminders(patch: JsonObject) {
        enqueue(patch)
        patches += patch
    }

    override suspend fun reschedule() {
        reschedules++
    }

    override suspend fun consumeFirstTripOffer(permission: ReminderPermission): Boolean? = false

    override suspend fun isCurrent() = current
}
