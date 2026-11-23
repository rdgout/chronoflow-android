package net.chronoflow.core.model.session

import net.chronoflow.core.model.launch.Gate

sealed class SessionFailure : Exception(null, null) {
    data object CredentialsUnavailable : SessionFailure()

    data object DatabaseUnavailable : SessionFailure()

    data object CredentialWrite : SessionFailure()

    data object InvalidResult : SessionFailure()

    data object Superseded : SessionFailure()
}

sealed interface SignOutResult {
    data object SignedOut : SignOutResult

    data class NeedsConfirmation(
        val pendingCount: Int,
    ) : SignOutResult
}

data class SessionSnapshot(
    val session: StoredSession,
    val epoch: Long,
)

data class SessionUpdate(
    val presence: SessionPresence,
    val epoch: Long,
)

data class StoredAccountReadiness(
    val accountId: String,
    val gate: Gate?,
    val hasStoredData: Boolean,
    val firstLoadGate: Gate = Gate.App,
)
