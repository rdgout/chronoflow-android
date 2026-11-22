package net.chronoflow.core.data.reminders

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.testing.reminders.reminderAccount
import net.chronoflow.core.testing.reminders.reminderNow
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = TestReminderApplication::class)
class ReminderDeliveryTest {
    @Test fun postsWithTagIdChannelAndTimeout() =
        fixture { app, runtime, intent ->
            val request = adapterRequest()
            shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
            ReminderDelivery.deliver(app, intent, runtime, runtime.clock)
            val notification =
                shadowOf(checkNotNull(app.getSystemService(NotificationManager::class.java)))
                    .getNotification(request.identifier, 7)
            assertNotNull(notification)
            assertEquals(CHANNEL_ID, notification.channelId)
            assertEquals(request.title, notification.extras.getString(Notification.EXTRA_TITLE))
            assertEquals(request.body, notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT))
            assertEquals(Notification.CATEGORY_REMINDER, notification.category)
            assertTrue(notification.flags and Notification.FLAG_AUTO_CANCEL != 0)
            assertEquals(1_200_000L, notification.timeoutAfter)
            val tap = shadowOf(notification.contentIntent).savedIntent
            assertEquals(runtime.tapComponent, tap.component)
            assertEquals(ACTION_OPEN, tap.action)
            assertEquals(request.identifier, tap.identifier)
            assertEquals(request.tripID, tap.getStringExtra(EXTRA_TRIP_ID))
            assertEquals(request.blockID, tap.getStringExtra(EXTRA_BLOCK_ID))
            assertEquals(request.inputHash, tap.getStringExtra(EXTRA_INPUT_HASH))
            assertEquals(request.accountID, tap.getStringExtra(EXTRA_ACCOUNT_ID))
            assertTrue(notification.contentIntent.isImmutable)
            assertEquals(listOf(reminderAccount to request.identifier), runtime.deliveries)
        }

    @Test fun expiredAlarmIsDropped() =
        fixture { app, runtime, intent ->
            intent.putExtra(EXTRA_EXPIRES_AT, reminderNow.toEpochMilli())
            ReminderDelivery.deliver(app, intent, runtime, runtime.clock)
            assertEquals(0, runtime.accountReads)
            assertEmpty(app, runtime)
        }

    @Test fun otherAccountOrSignedOutIsDropped() =
        fixture { app, runtime, intent ->
            shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
            runtime.account = "other"
            ReminderDelivery.deliver(app, intent, runtime, runtime.clock)
            assertEmpty(app, runtime)
            runtime.account = null
            ReminderDelivery.deliver(app, intent, runtime, runtime.clock)
            assertEmpty(app, runtime)
            intent.removeExtra(EXTRA_ACCOUNT_ID)
            ReminderDelivery.deliver(app, intent, runtime, runtime.clock)
            assertEmpty(app, runtime)
        }

    @Test fun missingPermissionIsDropped() =
        fixture { app, runtime, intent ->
            ReminderDelivery.deliver(app, intent, runtime, runtime.clock)
            assertEmpty(app, runtime)
            shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
            shadowOf(checkNotNull(app.getSystemService(NotificationManager::class.java))).setNotificationsEnabled(false)
            ReminderDelivery.deliver(app, intent, runtime, runtime.clock)
            assertEmpty(app, runtime)
        }

    @Test fun missingRequiredMetadataIsDropped() =
        fixture { app, runtime, intent ->
            shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
            intent.removeExtra(EXTRA_REQUEST_CODE)
            ReminderDelivery.deliver(app, intent, runtime, runtime.clock)
            assertEmpty(app, runtime)
        }

    private fun assertEmpty(
        app: TestReminderApplication,
        runtime: FakeReminderRuntime,
    ) {
        assertTrue(checkNotNull(app.getSystemService(NotificationManager::class.java)).activeNotifications.isEmpty())
        assertTrue(runtime.deliveries.isEmpty())
    }

    private fun fixture(test: suspend TestScope.(TestReminderApplication, FakeReminderRuntime, Intent) -> Unit) =
        runTest(timeout = 60.seconds) {
            val app = ApplicationProvider.getApplicationContext<TestReminderApplication>()
            val dispatcher = StandardTestDispatcher(testScheduler)
            val runtime = FakeReminderRuntime(app, dispatcher, backgroundScope)
            app.reminderRuntime = runtime
            val request = adapterRequest().copy(expiresAt = reminderNow.plusSeconds(1200))
            val intent = AndroidReminderAlarms(app, dispatcher).alarmIntent(request, 7)
            test(app, runtime, intent)
        }
}
