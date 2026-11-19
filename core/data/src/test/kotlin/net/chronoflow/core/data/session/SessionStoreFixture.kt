package net.chronoflow.core.data.session

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.withContext
import net.chronoflow.core.database.testing.JdbcAccountDriverFactory
import net.chronoflow.core.database.testing.TestRoots
import net.chronoflow.core.model.session.StorageNamespace
import net.chronoflow.core.sync.session.MemorySessionStore
import net.chronoflow.core.sync.session.SessionController
import net.chronoflow.core.sync.session.SessionTeardown
import java.io.File

internal class SessionStoreFixture private constructor(
    val root: File,
    val io: CoroutineDispatcher,
) {
    val credentials = MemorySessionStore()
    val accounts = SessionAccountDatabase({ root }, StorageNamespace.Mock, JdbcAccountDriverFactory(), io)
    val controller = SessionController(credentials, accounts, SessionTeardown(sleep = { awaitCancellation() }), io)

    suspend fun close() {
        accounts.close()
        controller.close()
        withContext(io) { check(root.deleteRecursively()) }
    }

    companion object {
        suspend operator fun invoke(io: CoroutineDispatcher): SessionStoreFixture =
            SessionStoreFixture(withContext(io) { TestRoots.temporaryStoreRoot() }, io)
    }
}
