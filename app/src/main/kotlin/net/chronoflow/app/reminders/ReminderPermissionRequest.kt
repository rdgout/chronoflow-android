package net.chronoflow.app.reminders

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import net.chronoflow.core.datastore.DevicePreferences
import net.chronoflow.feature.reminders.PermissionRequestStart

internal suspend fun beginReminderPermissionRequest(
    preferences: DevicePreferences,
    io: CoroutineDispatcher,
    sdkInt: Int,
    override: ReminderPermissionOverride?,
    runtimeGranted: () -> Boolean,
): PermissionRequestStart =
    withContext(io) {
        if (override != null) {
            override.requestPermission()
            return@withContext PermissionRequestStart.NoDialog
        }
        preferences.setNotificationPermissionRequested(true)
        if (sdkInt >= 33 && !runtimeGranted()) PermissionRequestStart.SystemDialog else PermissionRequestStart.NoDialog
    }
