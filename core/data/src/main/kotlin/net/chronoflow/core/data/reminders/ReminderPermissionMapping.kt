package net.chronoflow.core.data.reminders

import net.chronoflow.core.reminders.ReminderPermission

fun reminderPermission(
    sdkInt: Int,
    managerAvailable: Boolean,
    runtimeGranted: Boolean,
    requestedBefore: Boolean,
    notificationsEnabled: Boolean,
    channelBlocked: Boolean,
): ReminderPermission =
    when {
        !managerAvailable -> ReminderPermission.Unsupported
        !requestedBefore && (sdkInt < 33 || !runtimeGranted) -> ReminderPermission.Undetermined
        sdkInt >= 33 && !runtimeGranted -> ReminderPermission.Blocked
        !notificationsEnabled || channelBlocked -> ReminderPermission.Blocked
        else -> ReminderPermission.Granted
    }
