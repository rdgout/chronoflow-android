package net.chronoflow.core.data.reminders

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ReminderDeliveryGate {
    // The final account read and post must not straddle the sign-out wipe marker.
    private val mutex = Mutex()

    suspend fun postIfCurrent(
        accountId: String,
        storedAccountId: suspend () -> String?,
        post: () -> Unit,
    ): Boolean =
        mutex.withLock {
            if (accountId != storedAccountId()) return@withLock false
            post()
            true
        }

    suspend fun beginWipe(writeMarker: suspend () -> Unit) =
        mutex.withLock {
            writeMarker()
        }
}
