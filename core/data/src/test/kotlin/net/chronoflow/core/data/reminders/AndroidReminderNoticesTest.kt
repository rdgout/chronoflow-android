package net.chronoflow.core.data.reminders

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class AndroidReminderNoticesTest {
    @Test fun channelIsCreatedAndRelocalized() =
        runTest(timeout = 60.seconds) {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val manager = checkNotNull(context.getSystemService(NotificationManager::class.java))
            val dispatcher = StandardTestDispatcher(testScheduler)
            val notices = AndroidReminderNotices(context, dispatcher) { channelName(context, it) }
            notices.ensureChannel(Language.En)
            val channel = manager.getNotificationChannel(CHANNEL_ID)
            assertEquals(NotificationManager.IMPORTANCE_DEFAULT, channel.importance)
            assertEquals(channelName(context, Language.En), channel.name)
            channel.importance = NotificationManager.IMPORTANCE_NONE
            manager.createNotificationChannel(channel)
            notices.ensureChannel(Language.Nl)
            assertEquals(channelName(context, Language.Nl), manager.getNotificationChannel(CHANNEL_ID).name)
            assertEquals(NotificationManager.IMPORTANCE_NONE, manager.getNotificationChannel(CHANNEL_ID).importance)
        }

    @Test fun removeDeliveredRemovesOnlyReminders() =
        runTest(timeout = 60.seconds) {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val manager = checkNotNull(context.getSystemService(NotificationManager::class.java))
            val dispatcher = StandardTestDispatcher(testScheduler)
            val notices = AndroidReminderNotices(context, dispatcher) { channelName(context, it) }
            notices.ensureChannel(Language.En)
            val otherChannel = NotificationChannel("other", "Other", NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(otherChannel)
            val reminder =
                Notification
                    .Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .build()
            manager.notify("${TAG_PREFIX}a", 1, reminder)
            manager.notify("${TAG_PREFIX}b", 2, reminder)
            val other =
                Notification
                    .Builder(context, "other")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .build()
            manager.notify("unrelated", 3, other)
            notices.removeDelivered()
            assertEquals(listOf("unrelated"), manager.activeNotifications.map { it.tag })
        }
}
