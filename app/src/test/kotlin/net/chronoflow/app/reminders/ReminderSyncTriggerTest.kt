@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.app.reminders

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.sync.SyncEvent
import net.chronoflow.core.sync.SyncResult
import net.chronoflow.core.sync.SyncState
import net.chronoflow.core.testing.reminders.reminderAccount
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class ReminderSyncTriggerTest {
    @Test fun idleFinishedReschedulesAndOtherEventsDoNot() =
        runTest(timeout = 60.seconds) {
            val events = MutableSharedFlow<SyncEvent>()
            var count = 0
            backgroundScope.launch { rescheduleRemindersAfterSync(events) { count++ } }
            runCurrent()
            val id = AccountID.of(reminderAccount)
            events.emit(SyncEvent.Started(id, 1))
            runCurrent()
            assertEquals(0, count)
            events.emit(SyncEvent.Finished(id, 1, SyncResult(SyncState.Offline, 0)))
            runCurrent()
            assertEquals(0, count)
            events.emit(SyncEvent.Finished(id, 1, SyncResult(SyncState.Idle, 0)))
            runCurrent()
            assertEquals(1, count)
        }
}
