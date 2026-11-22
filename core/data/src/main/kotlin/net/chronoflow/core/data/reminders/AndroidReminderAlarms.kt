package net.chronoflow.core.data.reminders

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import net.chronoflow.core.reminders.ReminderRequest

class AndroidReminderAlarms(
    context: Context,
    private val io: CoroutineDispatcher,
    private val sdkInt: Int = Build.VERSION.SDK_INT,
) : ReminderAlarms {
    private val app = context.applicationContext

    private fun identity(identifier: String): Intent =
        Intent(ACTION_ALARM)
            .setComponent(ComponentName(app, ReminderAlarmReceiver::class.java))
            .setIdentifier(identifier)

    fun alarmIntent(
        request: ReminderRequest,
        code: Int,
    ): Intent =
        identity(request.identifier)
            .putExtra(EXTRA_IDENTIFIER, request.identifier)
            .putExtra(EXTRA_REQUEST_CODE, code)
            .putExtra(EXTRA_TITLE, request.title)
            .putExtra(EXTRA_BODY, request.body)
            .putExtra(EXTRA_TRIP_ID, request.tripID)
            .putExtra(EXTRA_BLOCK_ID, request.blockID)
            .putExtra(EXTRA_INPUT_HASH, request.inputHash)
            .putExtra(EXTRA_EXPIRES_AT, request.expiresAt.toEpochMilli())
            .apply { request.accountID?.let { putExtra(EXTRA_ACCOUNT_ID, it) } }

    @SuppressLint("ScheduleExactAlarm", "MissingPermission") // Exact-alarm permission does not exist below API 31.
    override suspend fun set(request: ReminderRequest, requestCode: Int) =
        withContext(io) {
            refusing {
                val manager = checkNotNull(app.getSystemService(AlarmManager::class.java))
                val pending =
                    PendingIntent.getBroadcast(
                        app,
                        requestCode,
                        alarmIntent(request, requestCode),
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    )
                if (sdkInt < 31) {
                    manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, request.sendAt.toEpochMilli(), pending)
                } else {
                    manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, request.sendAt.toEpochMilli(), pending)
                }
            }
        }

    override suspend fun cancel(
        identifier: String,
        requestCode: Int,
    ) = withContext(io) {
        val pending =
            PendingIntent.getBroadcast(
                app,
                requestCode,
                identity(identifier),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
            )
        if (pending != null) {
            checkNotNull(app.getSystemService(AlarmManager::class.java)).cancel(pending)
            pending.cancel()
        }
    }
}

internal inline fun refusing(action: () -> Unit) {
    try {
        action()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (
        error: IllegalStateException,
    ) {
        throw ReminderAlarmRefused(error)
    } catch (error: SecurityException) {
        throw ReminderAlarmRefused(error)
    }
}
