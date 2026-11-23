@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.core.data.reminders

import android.content.ComponentName
import android.content.Intent
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkManager
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = TestReminderApplication::class)
class ReminderUpkeepReceiverTest {
    @Test fun everyUpkeepActionEnqueuesOneReplacingReschedule() =
        runTest(timeout = 60.seconds) {
            val (app, runtime) = initializeReminderWork()
            try {
                fun assertAction(
                    action: String,
                    count: Int,
                ) {
                    app.sendBroadcast(
                        Intent(action).setComponent(ComponentName(app, ReminderUpkeepReceiver::class.java)),
                    )
                    shadowOf(Looper.getMainLooper()).idle()
                    runCurrent()
                    assertEquals(
                        1,
                        WorkManager
                            .getInstance(app)
                            .getWorkInfosForUniqueWork(WORK_RESCHEDULE)
                            .get()
                            .size,
                    )
                    assertEquals(count, runtime.passes)
                }
                assertAction(Intent.ACTION_BOOT_COMPLETED, 1)
                assertAction(Intent.ACTION_MY_PACKAGE_REPLACED, 2)
                assertAction(Intent.ACTION_TIMEZONE_CHANGED, 3)
                assertAction(Intent.ACTION_TIME_CHANGED, 4)
                assertAction(Intent.ACTION_LOCALE_CHANGED, 5)
            } finally {
                closeReminderWork(app)
            }
        }

    @Test fun unknownActionDoesNothing() =
        runTest(timeout = 60.seconds) {
            val (app, runtime) = initializeReminderWork()
            try {
                ReminderUpkeepReceiver().onReceive(app, Intent("unknown"))
                runCurrent()
                assertTrue(
                    WorkManager
                        .getInstance(app)
                        .getWorkInfosForUniqueWork(WORK_RESCHEDULE)
                        .get()
                        .isEmpty(),
                )
                assertEquals(0, runtime.passes)
            } finally {
                closeReminderWork(app)
            }
        }
}
