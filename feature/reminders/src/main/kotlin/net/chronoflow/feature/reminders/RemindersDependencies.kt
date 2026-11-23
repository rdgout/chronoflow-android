package net.chronoflow.feature.reminders

import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonObject
import net.chronoflow.core.reminders.ReminderPermission
import net.chronoflow.core.reminders.ReminderSettings

interface RemindersDependencies {
    fun reminderSettings(): Flow<ReminderSettings?>

    fun settingsChanges(): Flow<Unit>

    suspend fun permission(): ReminderPermission

    suspend fun beginPermissionRequest(): PermissionRequestStart

    suspend fun enqueueReminders(patch: JsonObject)

    suspend fun reschedule()

    suspend fun consumeFirstTripOffer(permission: ReminderPermission): Boolean?

    suspend fun isCurrent(): Boolean
}

enum class PermissionRequestStart { SystemDialog, NoDialog }

object RemindersDependenciesKey : CreationExtras.Key<RemindersDependencies>

object RemindersMainKey : CreationExtras.Key<CoroutineDispatcher>
