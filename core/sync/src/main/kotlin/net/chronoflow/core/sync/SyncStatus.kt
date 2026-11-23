package net.chronoflow.core.sync

import net.chronoflow.core.model.AccountID
import java.time.Instant

enum class SyncState { Idle, Offline, VerificationRequired, Unauthorized, Failed }

data class SyncResult(
    val state: SyncState,
    val pendingCount: Int,
    val retryAfter: Double? = null,
    val serverRetryAfter: Double? = null,
    val refusals: List<SyncRefusal> = emptyList(),
    val changes: SyncChanges = SyncChanges(),
)

sealed interface SyncEvent {
    data class Started(
        val accountID: AccountID,
        val epoch: Long,
    ) : SyncEvent

    data class Finished(
        val accountID: AccountID,
        val epoch: Long,
        val result: SyncResult,
    ) : SyncEvent
}

class SyncScheduler(
    val now: () -> Instant,
    val sleep: suspend (Double) -> Unit,
) {
    companion object
}
