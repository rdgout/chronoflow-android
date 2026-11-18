package net.chronoflow.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.chronoflow.core.data.repository.AccountModel
import net.chronoflow.core.data.repository.AccountRepository
import net.chronoflow.core.data.repository.OutboxRepository
import net.chronoflow.core.data.repository.SettingsRepository
import net.chronoflow.core.data.repository.TripRepository
import net.chronoflow.core.data.session.SessionAccountDatabase
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.model.session.StoredSession
import net.chronoflow.core.sync.SessionSync
import net.chronoflow.core.sync.session.SessionController

class SessionGraphs(
    private val controller: SessionController,
    private val sessionAccounts: SessionAccountDatabase,
    private val accountModel: AccountModel,
    private val sync: SessionSync,
    private val source: suspend (StoredSession) -> DataSource,
    private val scope: CoroutineScope,
) {
    private val mutableCurrent = MutableStateFlow<SessionGraph?>(null)
    val current = mutableCurrent.asStateFlow()
    private val context = scope.coroutineContext.minusKey(Job)
    private var observer: Job? = null

    fun start() {
        if (observer != null) return
        observer =
            scope.launch {
                controller.updates.filterNotNull().collect { update ->
                    val token = accountModel.attachmentToken
                    val database = sessionAccounts.activeAccountDatabase()
                    val snapshot = controller.activeSession()
                    if (controller.epoch != update.epoch || controller.presence != update.presence) return@collect
                    val signedIn = update.presence as? SessionPresence.SignedIn
                    val databaseMatches = signedIn != null && database?.accountID?.rawValue == signedIn.accountId
                    if (databaseMatches && database != null && snapshot != null && snapshot.epoch == update.epoch) {
                        val dataSource = source(snapshot.session)
                        if (controller.activeSession() != snapshot ||
                            accountModel.attachmentToken != token
                        ) {
                            return@collect
                        }
                        mutableCurrent.value?.scope?.cancel()
                        val graph =
                            SessionGraph(
                                update.epoch,
                                snapshot,
                                database,
                                AccountRepository(database),
                                TripRepository(database),
                                SettingsRepository(database),
                                OutboxRepository(database),
                                sync,
                                dataSource,
                                CoroutineScope(scope.coroutineContext + SupervisorJob(scope.coroutineContext[Job])),
                            )
                        mutableCurrent.value = graph
                        accountModel.attach(graph.accounts, update.epoch, token)
                    } else if (accountModel.attachmentToken == token) {
                        drop()
                    }
                }
            }
    }

    suspend fun drop() =
        withContext(NonCancellable + context) {
            mutableCurrent.value?.scope?.cancel()
            mutableCurrent.value = null
            accountModel.detach()
        }

    suspend fun close() {
        observer?.cancel()
        observer = null
        drop()
    }
}
