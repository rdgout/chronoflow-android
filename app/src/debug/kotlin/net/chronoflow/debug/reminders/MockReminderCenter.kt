package net.chronoflow.debug.reminders

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.chronoflow.app.reminders.ReminderPermissionOverride
import net.chronoflow.core.reminders.ReminderPermission

class MockReminderCenter(
    status: ReminderPermission,
) : ReminderPermissionOverride {
    private val mutex = Mutex()
    private var status = status

    @Volatile var permissionRequestCount = 0
        private set

    suspend fun setStatus(status: ReminderPermission) {
        mutex.withLock { this.status = status }
    }

    override suspend fun permission() = mutex.withLock { status }

    override suspend fun requestPermission() =
        mutex.withLock {
            permissionRequestCount++
            if (status == ReminderPermission.Undetermined) status = ReminderPermission.Granted
            status
        }
}
