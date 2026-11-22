package net.chronoflow.core.data.reminders

import android.annotation.SuppressLint
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = TestReminderApplication::class)
class ReminderWorkTest {
    @SuppressLint("RestrictedApi")
    @Test
    fun expeditedOnlyFromApi31() {
        assertFalse(ReminderWork.rescheduleRequest(30).workSpec.expedited)
        assertTrue(ReminderWork.rescheduleRequest(31).workSpec.expedited)
    }

    @Test fun refillIsUniqueAndCancelStopsBothNames() =
        runTest(timeout = 60.seconds) {
            val (app, _) = initializeReminderWork()
            try {
                ReminderWork.ensureRefill(app)
                ReminderWork.ensureRefill(app)
                val manager = WorkManager.getInstance(app)
                val refill = manager.getWorkInfosForUniqueWork(WORK_REFILL).get().single()
                assertEquals(43_200_000L, refill.periodicityInfo?.repeatIntervalMillis)
                ReminderWork.enqueueReschedule(app)
                ReminderWork.cancelAll(app)
                assertTrue(
                    manager.getWorkInfosForUniqueWork(WORK_REFILL).get().all { it.state == WorkInfo.State.CANCELLED },
                )
                assertTrue(manager.getWorkInfosForUniqueWork(WORK_RESCHEDULE).get().all { it.state.isFinished })
            } finally {
                closeReminderWork(app)
            }
        }
}
