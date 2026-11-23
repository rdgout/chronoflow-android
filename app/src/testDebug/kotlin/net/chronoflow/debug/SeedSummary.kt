package net.chronoflow.debug

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import net.chronoflow.core.data.repository.AccountRepository
import net.chronoflow.core.data.repository.OutboxRepository
import net.chronoflow.core.data.repository.TripRepository
import net.chronoflow.core.data.session.AccountReadinessRepository
import net.chronoflow.core.data.session.SessionAccountDatabase
import net.chronoflow.core.database.testing.JdbcAccountDriverFactory
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.session.SessionStore
import net.chronoflow.core.model.session.StorageNamespace
import net.chronoflow.core.model.session.readStoredSession
import java.io.File

internal data class SeedSummary(
    val id: String,
    val email: String,
    val walkthrough: Boolean,
    val verified: Boolean,
    val trips: List<String>,
    val pending: Int,
    val synced: Boolean,
)

internal suspend fun seedSummary(
    root: File,
    credentials: SessionStore,
    io: CoroutineDispatcher,
): SeedSummary {
    val stored = checkNotNull(credentials.readStoredSession())
    val accounts = SessionAccountDatabase({ root }, StorageNamespace.Mock, JdbcAccountDriverFactory(), io)
    try {
        accounts.open(AccountID.of(stored.accountId))
        val db = checkNotNull(accounts.activeAccountDatabase())
        val account = checkNotNull(AccountRepository(db).account())
        return SeedSummary(
            account.id,
            account.email,
            account.walkthroughSeen,
            account.hasVerifiedEmail,
            TripRepository(db).trips().map { it.id }.sorted(),
            OutboxRepository(db).pending().size,
            checkNotNull(AccountReadinessRepository(db).values(stored.accountId).first()).hasStoredData,
        )
    } finally {
        accounts.close()
    }
}
