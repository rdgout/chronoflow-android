package net.chronoflow.core.data.reminders

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.chronoflow.core.database.AccountDatabase
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.plan.PlanDecoder
import net.chronoflow.core.reminders.ReminderLedger
import net.chronoflow.core.reminders.ReminderLedgerEntry
import net.chronoflow.core.reminders.ReminderPermission
import net.chronoflow.core.reminders.ReminderSettings
import net.chronoflow.core.reminders.ReminderStrings
import net.chronoflow.core.reminders.from
import net.chronoflow.core.reminders.reminderRequests
import net.chronoflow.core.reminders.reminderTimeFormat
import java.io.IOException
import java.security.GeneralSecurityException
import java.time.Instant

class ReminderService(
    private val alarms: ReminderAlarms,
    private val notices: ReminderNotices,
    private val permission: suspend () -> ReminderPermission,
    private val language: suspend () -> Language,
    private val strings: ReminderStrings,
    private val formatter: suspend () -> CopyLineFormatter,
    private val decoder: PlanDecoder,
    private val clock: () -> Instant,
    dispatcher: CoroutineDispatcher,
) {
    private val confined = dispatcher.limitedParallelism(1)
    private val lifetime = SupervisorJob()
    private val scope = CoroutineScope(lifetime + confined)
    private var database: AccountDatabase? = null
    private var observation: Job? = null
    private var flight: Job? = null
    private var rerun = false
    private var generation = 0L
    private var armedInThisProcess = false
    private val mutableAttachedAccount = MutableStateFlow<String?>(null)
    val attachedAccount = mutableAttachedAccount.asStateFlow()
    var failure: Throwable? = null
        private set

    suspend fun attach(database: AccountDatabase) =
        withContext(confined) {
            if (this@ReminderService.database === database) return@withContext
            detach()
            this@ReminderService.database = database
            val gen = generation
            observation =
                scope.launch {
                    try {
                        database
                            .observe("settings", "trips", "plan_versions", "check_marks") { ReminderInputs.fetch(it) }
                            .distinctUntilChanged()
                            .collect {
                                if (generation == gen) reschedule()
                            }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: StoreFailure.Closed) {
                    }
                }
            mutableAttachedAccount.value = database.accountID?.rawValue
        }

    suspend fun reschedule() =
        withContext(confined) {
            rerun = true
            val current = flight ?: scope.launch { run() }.also { flight = it }
            current.join()
        }

    private suspend fun run() {
        try {
            do {
                rerun = false
                val gen = generation
                try {
                    schedule(gen)
                } catch (cancelled: CancellationException) {
                    if (!currentCoroutineContext().isActive) throw cancelled
                    failure = cancelled
                } catch (error: StoreFailure) {
                    failure = error
                } catch (error: IOException) {
                    failure = error
                } catch (error: GeneralSecurityException) {
                    failure = error
                } catch (error: RuntimeException) {
                    failure = error
                }
            } while (rerun)
        } finally {
            flight = null
        }
    }

    private suspend fun schedule(gen: Long) {
        val db = database ?: return
        val id = db.accountID ?: return
        val now = clock()
        val rows = ReminderStore.rows(db)
        val ledger = ReminderLedger.elapsed(ReminderStore.ledger(db), now).toMutableList()
        val inputs = db.read { ReminderInputs.fetch(it) }
        val settings = ReminderSettings.decode(inputs.reminders)
        val permission = permission()
        if (generation != gen) return
        val active = settings != null && permission.remindersActive(settings)
        val language = if (active) language() else null
        val upcoming =
            if (active) {
                reminderRequests(
                    inputs.plans(decoder),
                    id.rawValue,
                    checkNotNull(settings),
                    ledger,
                    now,
                    checkNotNull(language),
                    reminderTimeFormat(inputs.preferences),
                    formatter(),
                    strings,
                )
            } else {
                emptyList()
            }
        if (generation != gen) return
        if (upcoming.isNotEmpty()) notices.ensureChannel(checkNotNull(language))
        val desired = upcoming.map { it.identifier }.toSet()
        for (row in rows.filter { it.identifier !in desired }) {
            if (row.expiresAt <= now) {
                ReminderStore.delete(db, row.identifier)
            } else if (row.sendAt > now || !active) {
                alarms.cancel(row.identifier, row.requestCode)
                ReminderStore.delete(db, row.identifier)
            }
        }
        val taken = rows.map { it.requestCode }.toMutableSet()
        val existingById = rows.associateBy { it.identifier }
        var completed = true
        for (request in upcoming) {
            if (generation != gen || rerun) {
                completed = false
                break
            }
            val existing = existingById[request.identifier]
            if (existing == null || !existing.sameContent(request) || !armedInThisProcess) {
                val code = existing?.requestCode ?: ReminderRequestCodes.allocate(request.identifier, taken)
                taken += code
                try {
                    alarms.set(request, code)
                } catch (_: ReminderAlarmRefused) {
                    continue
                }
                ReminderStore.save(db, ScheduledReminderRow.of(request, code))
            }
            ledger += ReminderLedgerEntry.from(request)
        }
        if (generation != gen) return
        ReminderStore.replaceLedger(db, ledger)
        if (completed) armedInThisProcess = true
        failure = null
    }

    suspend fun detach() =
        withContext(NonCancellable + confined) {
            generation += 1
            rerun = false
            val previous = observation
            observation = null
            previous?.cancel()
            val db = database
            database = null
            mutableAttachedAccount.value = null
            flight?.join()
            previous?.join()
            if (db == null) return@withContext
            try {
                ReminderStore.rows(db).forEach { alarms.cancel(it.identifier, it.requestCode) }
                ReminderStore.deleteAll(db)
            } catch (_: StoreFailure.Closed) {
            }
            notices.removeDelivered()
            try {
                ReminderStore.clearLedger(db)
            } catch (_: StoreFailure.Closed) {
            }
            armedInThisProcess = false
        }

    suspend fun delivered(
        accountId: String,
        identifier: String,
    ) = withContext(confined) {
        val db = database ?: return@withContext
        if (db.accountID?.rawValue != accountId) return@withContext
        val row = ReminderStore.rows(db).firstOrNull { it.identifier == identifier } ?: return@withContext
        ReminderStore.delete(db, identifier)
        ReminderStore.replaceLedger(
            db,
            ReminderLedger.elapsed(ReminderStore.ledger(db), clock()) + ReminderLedgerEntry.from(row.toRequest()),
        )
    }

    suspend fun close() =
        withContext(NonCancellable) {
            detach()
            lifetime.cancelAndJoin()
        }
}
