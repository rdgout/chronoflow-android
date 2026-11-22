@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.core.data.reminders

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.IntentFilter
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowBroadcastPendingResult
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = TestReminderApplication::class)
class ReminderAlarmReceiverTest {
    @Test fun broadcastPostsAndFinishesPendingResult() =
        runTest(timeout = 60.seconds) {
            val app = ApplicationProvider.getApplicationContext<TestReminderApplication>()
            val dispatcher = StandardTestDispatcher(testScheduler)
            val runtime = FakeReminderRuntime(app, dispatcher, backgroundScope)
            app.reminderRuntime = runtime
            shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
            val receiver = ReminderAlarmReceiver()
            app.registerReceiver(receiver, IntentFilter(ACTION_ALARM), Context.RECEIVER_NOT_EXPORTED)
            try {
                val intent = AndroidReminderAlarms(app, dispatcher).alarmIntent(adapterRequest(), 7).setComponent(null)
                app.sendBroadcast(intent)
                shadowOf(Looper.getMainLooper()).idle()
                runCurrent()
                assertEquals(
                    1,
                    checkNotNull(app.getSystemService(NotificationManager::class.java)).activeNotifications.size,
                )
                assertTrue(shadowOf(receiver).wentAsync())
                val result = checkNotNull(shadowOf(receiver).originalPendingResult)
                assertTrue(Shadow.extract<ShadowBroadcastPendingResult>(result).future.isDone)
            } finally {
                app.unregisterReceiver(receiver)
            }
        }

    @Test fun wrongActionDoesNothing() =
        runTest(timeout = 60.seconds) {
            val app = ApplicationProvider.getApplicationContext<TestReminderApplication>()
            val dispatcher = StandardTestDispatcher(testScheduler)
            val runtime = FakeReminderRuntime(app, dispatcher, backgroundScope)
            app.reminderRuntime = runtime
            val receiver = ReminderAlarmReceiver()
            receiver.onReceive(app, android.content.Intent("other"))
            runCurrent()
            assertEquals(0, runtime.accountReads)
            assertTrue(runtime.deliveries.isEmpty())
        }
}
