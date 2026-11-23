package net.chronoflow.app

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
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
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val copyCatalogs: net.chronoflow.core.plan.CopyCatalogProvider? = null,
    private val attachReminders: suspend (net.chronoflow.core.database.AccountDatabase) -> Unit,
    private val detachReminders: suspend () -> Unit,
    private val clearReminderTaps: () -> Unit,
    private val ensureRefill: suspend () -> Unit,
    private val rescheduleReminders: suspend () -> Unit,
) {
    private val mutableCurrent = MutableStateFlow<SessionGraph?>(null)
    val current = mutableCurrent.asStateFlow()
    private val context = scope.coroutineContext.minusKey(Job)
    private var observer: Job? = null
    private var reminderEvents: Job? = null

    fun start() {
        if (observer != null) return
        reminderEvents =
            scope.launch {
                net.chronoflow.app.reminders
                    .rescheduleRemindersAfterSync(sync.events(), rescheduleReminders)
            }
        observer =
            scope.launch {
                controller.updates.filterNotNull().collect { update ->
                    val token = accountModel.attachmentToken
                    val database = sessionAccounts.activeAccountDatabase()
                    val snapshot = controller.activeSession()
                    if (controller.epoch != update.epoch || controller.presence != update.presence) return@collect
                    val signedIn = update.presence as? SessionPresence.SignedIn
                    val databaseMatches = signedIn != null && database?.accountID?.rawValue == signedIn.accountId
                    if (databaseMatches && snapshot != null && snapshot.epoch == update.epoch) {
                        val dataSource = source(snapshot.session)
                        val permit = controller.mutationPermit(snapshot)
                        if (controller.activeSession() != snapshot ||
                            accountModel.attachmentToken != token
                        ) {
                            return@collect
                        }
                        attachReminders(database)
                        if (controller.epoch != update.epoch || controller.presence != update.presence) {
                            detachReminders()
                            return@collect
                        }
                        withContext(io) { ensureRefill() }
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
                                permit = permit,
                                wizardIo = io,
                                wizardCatalogs = copyCatalogs,
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
            clearReminderTaps()
            detachReminders()
            mutableCurrent.value?.scope?.cancel()
            mutableCurrent.value = null
            accountModel.detach()
        }

    suspend fun close() =
        withContext(NonCancellable) {
            observer?.cancelAndJoin()
            observer = null
            reminderEvents?.cancelAndJoin()
            reminderEvents = null
            drop()
        }
}
