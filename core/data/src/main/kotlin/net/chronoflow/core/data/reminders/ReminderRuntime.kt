package net.chronoflow.core.data.reminders

import android.content.ComponentName
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import java.time.Instant

interface ReminderHost {
    val reminderRuntime: ReminderRuntime
}

interface ReminderRuntime {
    val io: CoroutineDispatcher
    val scope: CoroutineScope
    val notificationIcon: Int
    val tapComponent: ComponentName
    val clock: () -> Instant

    suspend fun storedAccountId(): String?

    suspend fun ensureChannel()

    suspend fun delivered(
        accountId: String,
        identifier: String,
    )

    suspend fun backgroundPass()
}
