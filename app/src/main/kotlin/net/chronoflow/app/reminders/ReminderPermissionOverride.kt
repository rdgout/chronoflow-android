package net.chronoflow.app.reminders

import net.chronoflow.core.reminders.ReminderPermission

interface ReminderPermissionOverride {
    suspend fun permission(): ReminderPermission

    suspend fun requestPermission(): ReminderPermission
}
