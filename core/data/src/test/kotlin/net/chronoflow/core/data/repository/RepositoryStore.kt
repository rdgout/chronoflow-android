package net.chronoflow.core.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import net.chronoflow.core.database.AccountDatabase
import net.chronoflow.core.database.testing.JdbcAccountDriverFactory
import net.chronoflow.core.database.testing.TestRoots
import net.chronoflow.core.testing.sync.syncAccount
import java.io.File

internal class RepositoryStore private constructor(
    val database: AccountDatabase,
    val root: File,
    val io: CoroutineDispatcher,
) {
    suspend fun close() {
        database.close()
        withContext(io) { check(root.deleteRecursively()) }
    }

    companion object {
        suspend operator fun invoke(
            io: CoroutineDispatcher,
            id: String = syncAccount,
        ): RepositoryStore {
            val root = withContext(io) { TestRoots.temporaryStoreRoot() }
            val database = AccountDatabase(root, driverFactory = JdbcAccountDriverFactory(), io = io)
            database.open(id)
            return RepositoryStore(database, root, io)
        }
    }
}
