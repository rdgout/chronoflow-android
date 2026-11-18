package net.chronoflow.core.data.session

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import net.chronoflow.core.data.store.upsertSessionAccount
import net.chronoflow.core.data.sync.SqlSyncDatabase
import net.chronoflow.core.database.AccountDatabase
import net.chronoflow.core.database.AccountDriverFactory
import net.chronoflow.core.database.store.fetchAccount
import net.chronoflow.core.database.store.saveAccount
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.api.SessionAccount
import net.chronoflow.core.model.session.StorageNamespace
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.core.sync.ActiveSyncDatabase
import net.chronoflow.core.sync.SyncDatabase
import net.chronoflow.core.sync.session.SessionAccountStore
import java.io.File

class SessionAccountDatabase(
    private val storageRoot: suspend () -> File,
    private val namespace: StorageNamespace,
    private val driverFactory: AccountDriverFactory,
    private val io: CoroutineDispatcher,
    private val detach: suspend () -> Unit = {},
) : SessionAccountStore,
    ActiveSyncDatabase {
    private val confined = io.limitedParallelism(1)
    private var database: AccountDatabase? = null
    private var closing = false

    suspend fun activeAccountDatabase(): AccountDatabase? = withContext(confined) { if (closing) null else database }

    override suspend fun activeDatabase(): SyncDatabase? = activeAccountDatabase()?.let(::SqlSyncDatabase)

    override suspend fun open(id: AccountID) =
        withContext(confined) {
            if (database?.accountID == id && !closing) return@withContext
            close()
            val candidate = AccountDatabase(storageRoot(), namespace, driverFactory, io)
            candidate.open(id.rawValue)
            database = candidate
        }

    override suspend fun upsert(account: SessionAccount) =
        withContext(confined) {
            val current = database ?: throw StoreFailure.InvalidAccountID
            if (closing) throw StoreFailure.InvalidAccountID
            current.upsertSessionAccount(account)
        }

    override suspend fun markEmailVerified(
        id: AccountID,
        email: String,
    ): Boolean =
        withContext(confined) {
            val current = database
            if (closing || current?.accountID != id) return@withContext false
            current.write { sql ->
                val record = sql.fetchAccount(id.rawValue)
                if (record == null || record.email != email) {
                    false
                } else {
                    sql.saveAccount(record.copy(hasVerifiedEmail = true))
                    true
                }
            }
        }

    override suspend fun close() =
        withContext(NonCancellable + confined) {
            closing = true
            try {
                detach()
                database?.close()
                database = null
            } finally {
                closing = false
            }
        }
}
