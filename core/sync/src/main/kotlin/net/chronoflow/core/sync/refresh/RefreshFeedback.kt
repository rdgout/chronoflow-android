package net.chronoflow.core.sync.refresh

import net.chronoflow.core.sync.SyncResult
import net.chronoflow.core.sync.SyncState

data class RefreshFeedback(
    val key: RefreshFeedbackKey,
    val count: Int? = null,
)

enum class RefreshFeedbackKey { UpToDate, TripsAdded, TripsUpdated, TripsRemoved, NeedsConnection, SomethingWentWrong }

fun outcome(
    result: SyncResult,
    userInitiated: Boolean,
): List<RefreshFeedback> {
    if (!userInitiated) return emptyList()
    return when (result.state) {
        SyncState.Offline -> {
            listOf(RefreshFeedback(RefreshFeedbackKey.NeedsConnection))
        }

        SyncState.Failed, SyncState.Unauthorized, SyncState.VerificationRequired -> {
            listOf(RefreshFeedback(RefreshFeedbackKey.SomethingWentWrong))
        }

        SyncState.Idle -> {
            if (result.retryAfter != null || result.pendingCount != 0 || result.refusals.isNotEmpty()) {
                listOf(RefreshFeedback(RefreshFeedbackKey.SomethingWentWrong))
            } else {
                val changes = result.changes
                val feedback =
                    listOf(
                        RefreshFeedback(RefreshFeedbackKey.TripsAdded, changes.tripsAdded),
                        RefreshFeedback(RefreshFeedbackKey.TripsUpdated, changes.tripsUpdated),
                        RefreshFeedback(RefreshFeedbackKey.TripsRemoved, changes.tripsRemoved),
                    ).filter { (it.count ?: 0) > 0 }
                feedback.ifEmpty { listOf(RefreshFeedback(RefreshFeedbackKey.UpToDate)) }
            }
        }
    }
}
