package net.chronoflow.core.data.reminders

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.contracts.Language

class AndroidReminderNotices(
    context: Context,
    private val io: CoroutineDispatcher,
    private val channelName: (Language) -> String,
) : ReminderNotices {
    private val app = context.applicationContext

    override suspend fun ensureChannel(language: Language) =
        withContext(io) {
            NotificationManagerCompat.from(app).createNotificationChannel(
                NotificationChannelCompat
                    .Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                    .setName(channelName(language))
                    .build(),
            )
        }

    override suspend fun removeDelivered() =
        withContext(io) {
            val manager = app.getSystemService(NotificationManager::class.java) ?: return@withContext
            manager.activeNotifications
                .filter {
                    it.notification.channelId == CHANNEL_ID ||
                        it.tag?.startsWith(TAG_PREFIX) == true
                }.forEach { manager.cancel(it.tag, it.id) }
        }
}
