package net.chronoflow.app

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import net.chronoflow.core.data.repository.AccountModel
import net.chronoflow.core.data.repository.OutboxRepository
import net.chronoflow.core.data.session.AccountWiper
import net.chronoflow.core.data.session.SessionAccountDatabase
import net.chronoflow.core.database.testing.JdbcAccountDriverFactory
import net.chronoflow.core.database.testing.TestRoots
import net.chronoflow.core.model.session.StorageNamespace
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.core.sync.SessionSync
import net.chronoflow.core.sync.SyncScheduler
import net.chronoflow.core.sync.session.MemorySessionStore
import net.chronoflow.core.sync.session.SessionController
import net.chronoflow.core.sync.session.SessionTeardown
import net.chronoflow.core.testing.sync.ManualSyncScheduler
import net.chronoflow.core.testing.sync.ScriptedSource
import net.chronoflow.core.testing.sync.syncDate
import java.io.File

internal class AppSessionFixture private constructor(
    val root: File,
    val io: CoroutineDispatcher,
    scope: CoroutineScope,
) {
    val credentials = MemorySessionStore()
    val model = AccountModel(scope)
    val accounts: SessionAccountDatabase =
        SessionAccountDatabase(
            { root },
            StorageNamespace.Mock,
            JdbcAccountDriverFactory(),
            io,
            detach = { graphs.drop() },
        )
    val wiper = AccountWiper(root, StorageNamespace.Mock, credentials, io = io)
    val source = ScriptedSource()
    val clock = ManualSyncScheduler()
    val controller: SessionController =
        SessionController(
            credentials,
            accounts,
            SessionTeardown(
                sleep = { awaitCancellation() },
                prepare = {
                    accounts.close()
                    wiper.recover()
                },
                pendingCount = {
                    OutboxRepository(
                        accounts.activeAccountDatabase() ?: throw StoreFailure.Closed,
                    ).pending().size
                },
                beginWipe = wiper::begin,
                erase = wiper::erase,
                finishWipe = wiper::finish,
            ),
            io,
        )
    val sync: SessionSync =
        SessionSync(
            accounts,
            SyncScheduler({ syncDate }, clock::sleep),
            source = { source },
            isCurrent = { s, e -> controller.activeSession()?.let { it.session == s && it.epoch == e } ?: false },
            unauthorized = { controller.unauthorized(it) },
            verification = {},
            dispatcher = io,
        )
    val graphs: SessionGraphs = SessionGraphs(controller, accounts, model, sync, { source }, scope)

    init {
        controller.attach(stopSync = sync::stop, requestSync = { sync.requestStart(controller, it) })
        graphs.start()
    }

    suspend fun close() {
        sync.stop()
        graphs.close()
        accounts.close()
        controller.close()
        ownedTestScope(controller).coroutineContext.job.cancelAndJoin()
        ownedTestScope(sync).coroutineContext.job.cancelAndJoin()
        withContext(io) { check(root.deleteRecursively()) }
    }

    companion object {
        suspend operator fun invoke(
            io: CoroutineDispatcher,
            scope: CoroutineScope,
        ): AppSessionFixture = AppSessionFixture(withContext(io) { TestRoots.temporaryStoreRoot() }, io, scope)
    }
}
