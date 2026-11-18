package net.chronoflow.core.data.sync.support

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import net.chronoflow.core.data.repository.AccountRepository
import net.chronoflow.core.data.repository.OutboxRepository
import net.chronoflow.core.data.repository.TripRepository
import net.chronoflow.core.data.sync.SqlSyncDatabase
import net.chronoflow.core.database.AccountDatabase
import net.chronoflow.core.database.testing.JdbcAccountDriverFactory
import net.chronoflow.core.database.testing.TestRoots
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.TripCardView
import net.chronoflow.core.model.session.SyncEpoch
import net.chronoflow.core.model.store.AccountRecord
import net.chronoflow.core.model.store.TripRecord
import net.chronoflow.core.testing.sync.card
import net.chronoflow.core.testing.sync.syncAccount
import net.chronoflow.core.testing.sync.syncDate
import net.chronoflow.core.testing.sync.syncTrip
import java.io.File

class SyncStore private constructor(
    val database: AccountDatabase,
    val root: File,
    val epoch: SyncEpoch,
    private val io: CoroutineDispatcher,
) {
    val sync = SqlSyncDatabase(database)

    suspend fun enqueue(
        change: OutboxChange,
        index: Byte = 0,
    ) = OutboxRepository(database).enqueue(
        change,
        now = syncDate,
        randomBytes =
            ByteArray(10) {
                index
            },
    )

    suspend fun close() {
        database.close()
        withContext(io) { check(root.deleteRecursively()) }
    }

    companion object {
        suspend operator fun invoke(
            io: CoroutineDispatcher,
            id: String = syncAccount,
        ): SyncStore {
            val root = withContext(io) { TestRoots.temporaryStoreRoot() }
            val database = AccountDatabase(root, driverFactory = JdbcAccountDriverFactory(), io = io)
            database.open(id)
            AccountRepository(database).upsert(AccountRecord(id, "Local", "demo@example.com"))
            TripRepository(
                database,
            ).upsert(
                TripRecord(
                    syncTrip,
                    ApiJson.response.encodeToString(TripCardView.serializer(), card().card),
                    currentInputHash = "hash",
                    departureUtc = "2026-11-10T10:00:00Z",
                ),
            )
            return SyncStore(database, root, SyncEpoch(AccountID.of(id), 1), io)
        }
    }
}
