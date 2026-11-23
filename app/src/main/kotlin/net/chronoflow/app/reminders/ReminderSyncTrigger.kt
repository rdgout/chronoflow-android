package net.chronoflow.app.reminders

import kotlinx.coroutines.flow.Flow
import net.chronoflow.core.sync.SyncEvent
import net.chronoflow.core.sync.SyncState

internal suspend fun rescheduleRemindersAfterSync(
    events: Flow<SyncEvent>,
    reschedule: suspend () -> Unit,
) {
    events.collect { event ->
        if (event is SyncEvent.Finished && event.result.state == SyncState.Idle) reschedule()
    }
}
