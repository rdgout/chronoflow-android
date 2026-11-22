package net.chronoflow.core.data.reminders

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.store.StoreFailure
import java.time.Instant

object ReminderDelivery {
    suspend fun deliver(
        context: Context,
        intent: Intent,
        runtime: ReminderRuntime,
        clock: () -> Instant,
    ) = withContext(runtime.io) {
        val identifier = intent.getStringExtra(EXTRA_IDENTIFIER) ?: return@withContext
        if (!intent.hasExtra(EXTRA_REQUEST_CODE) || !intent.hasExtra(EXTRA_EXPIRES_AT)) return@withContext
        val code = intent.getIntExtra(EXTRA_REQUEST_CODE, -1)
        if (code < 0) return@withContext
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return@withContext
        val body = intent.getStringExtra(EXTRA_BODY) ?: return@withContext
        val trip = intent.getStringExtra(EXTRA_TRIP_ID) ?: return@withContext
        val expiresAt = Instant.ofEpochMilli(intent.getLongExtra(EXTRA_EXPIRES_AT, 0))
        val now = clock()
        if (expiresAt <= now) return@withContext
        val account = intent.getStringExtra(EXTRA_ACCOUNT_ID) ?: return@withContext
        if (account != runtime.storedAccountId()) return@withContext
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return@withContext
        }
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return@withContext
        runtime.ensureChannel()
        val tap =
            Intent(ACTION_OPEN)
                .setComponent(runtime.tapComponent)
                .setIdentifier(identifier)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(EXTRA_TRIP_ID, trip)
                .putExtra(EXTRA_BLOCK_ID, intent.getStringExtra(EXTRA_BLOCK_ID))
                .putExtra(EXTRA_INPUT_HASH, intent.getStringExtra(EXTRA_INPUT_HASH))
                .putExtra(EXTRA_ACCOUNT_ID, account)
        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(runtime.notificationIcon)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setTimeoutAfter(expiresAt.toEpochMilli() - now.toEpochMilli())
                .setContentIntent(
                    PendingIntent.getActivity(
                        context,
                        code,
                        tap,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    ),
                ).build()
        try {
            manager.notify(identifier, code, notification)
        } catch (_: SecurityException) {
            return@withContext
        }
        try {
            runtime.delivered(account, identifier)
        } catch (_: StoreFailure.Closed) {
            // A sign-out may close the database after the notification is posted.
        }
    }
}
