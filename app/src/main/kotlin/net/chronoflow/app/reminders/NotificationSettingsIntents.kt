package net.chronoflow.app.reminders

import android.app.Activity
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import net.chronoflow.core.data.reminders.CHANNEL_ID

fun notificationSettingsIntent(
    packageName: String,
    appNotificationsEnabled: Boolean,
    channelBlocked: Boolean,
): Intent =
    if (appNotificationsEnabled && channelBlocked) {
        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            .putExtra(Settings.EXTRA_CHANNEL_ID, CHANNEL_ID)
    } else {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    }

internal suspend fun openNotificationSettings(
    context: Context,
    io: CoroutineDispatcher,
) {
    val intent =
        withContext(io) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val channelBlocked =
                manager?.getNotificationChannel(CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_NONE
            notificationSettingsIntent(
                context.packageName,
                NotificationManagerCompat.from(context).areNotificationsEnabled(),
                channelBlocked,
            )
        }
    startNotificationSettings(context, intent)
}

internal fun startNotificationSettings(
    context: Context,
    intent: Intent,
) {
    fun Intent.forContext(): Intent = apply { if (context.activity() == null) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    try {
        context.startActivity(intent.forContext())
    } catch (_: ActivityNotFoundException) {
        context.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null),
            ).forContext(),
        )
    }
}

private tailrec fun Context.activity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.takeUnless { it === this }?.activity()
        else -> null
    }
