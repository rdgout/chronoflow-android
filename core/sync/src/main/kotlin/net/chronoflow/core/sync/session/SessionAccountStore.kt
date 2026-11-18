package net.chronoflow.core.sync.session

import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.api.SessionAccount
import net.chronoflow.core.model.session.SessionFailure

interface SessionAccountStore {
    suspend fun open(id: AccountID)

    suspend fun upsert(account: SessionAccount)

    suspend fun close()

    suspend fun markEmailVerified(
        id: AccountID,
        email: String,
    ): Boolean = throw SessionFailure.DatabaseUnavailable
}
