package net.chronoflow.core.data.reminders

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import net.chronoflow.core.datastore.DevicePreferences
import net.chronoflow.core.reminders.ReminderPermission

class ReminderPermissions(
    context: Context,
    private val devicePreferences: DevicePreferences,
    private val io: CoroutineDispatcher,
    private val sdkInt: Int = Build.VERSION.SDK_INT,
) {
    private val app = context.applicationContext
    private val mutableState = MutableStateFlow<ReminderPermission?>(null)
    val state = mutableState.asStateFlow()

    suspend fun refresh(): ReminderPermission =
        withContext(io) {
            val manager = app.getSystemService(NotificationManager::class.java)
            val permission =
                reminderPermission(
                    sdkInt = sdkInt,
                    managerAvailable = manager != null,
                    runtimeGranted =
                        Build.VERSION.SDK_INT < 33 || sdkInt < 33 ||
                            ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) ==
                            PackageManager.PERMISSION_GRANTED,
                    requestedBefore = devicePreferences.notificationPermissionRequested.first(),
                    notificationsEnabled = NotificationManagerCompat.from(app).areNotificationsEnabled(),
                    channelBlocked =
                        manager?.getNotificationChannel(CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_NONE,
                )
            mutableState.value = permission
            permission
        }
}
