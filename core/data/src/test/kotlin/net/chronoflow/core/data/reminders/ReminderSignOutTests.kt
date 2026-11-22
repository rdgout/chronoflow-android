package net.chronoflow.core.data.reminders

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.data.repository.SettingsRepository
import net.chronoflow.core.data.repository.TripRepository
import net.chronoflow.core.data.session.AccountWiper
import net.chronoflow.core.data.session.SessionAccountDatabase
import net.chronoflow.core.database.AccountDatabase
import net.chronoflow.core.database.testing.JdbcAccountDriverFactory
import net.chronoflow.core.database.testing.TestRoots
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.session.SignOutResult
import net.chronoflow.core.model.session.StorageNamespace
import net.chronoflow.core.model.store.SettingsRecord
import net.chronoflow.core.sync.session.MemorySessionStore
import net.chronoflow.core.sync.session.SessionController
import net.chronoflow.core.sync.session.SessionTeardown
import net.chronoflow.core.sync.session.sessionResult
import net.chronoflow.core.testing.reminders.reminderAccount
import net.chronoflow.core.testing.reminders.reminderPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class ReminderSignOutTests {
    @Test fun signOutWipesNotificationsLedgerAndAccountDirectory() =
        runTest(timeout = 60.seconds) {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val root = TestRoots.temporaryStoreRoot()
            val alarms = FakeReminderAlarms()
            val notices = FakeReminderNotices()
            val service = reminderService(alarms, notices)
            val accounts =
                SessionAccountDatabase(
                    { root },
                    StorageNamespace.Mock,
                    JdbcAccountDriverFactory(),
                    dispatcher,
                    detach = { service.detach() },
                )
            val credentials = MemorySessionStore()
            val wiper = AccountWiper({ root }, StorageNamespace.Mock, credentials, io = dispatcher)
            val controller =
                SessionController(
                    credentials,
                    accounts,
                    SessionTeardown(sleep = {
                        delay(it)
                    }, beginWipe = wiper::begin, erase = wiper::erase, finishWipe = wiper::finish),
                    dispatcher,
                )
            controller.attach(stopSync = { service.detach() }, requestSync = {})
            val fixture = ReminderTestStore(dispatcher)
            try {
                controller.accept(sessionResult(reminderAccount))
                val snapshot = checkNotNull(controller.activeSession())
                val database = checkNotNull(accounts.activeAccountDatabase())
                fixture.store(reminderPlan())
                copyReminderPlan(fixture.database, database)
                service.attach(database)
                service.reschedule()
                assertEquals(1, ReminderStore.rows(database).size)
                assertTrue(ReminderStore.ledger(database).isNotEmpty())
                val directory = database.directory(AccountID.of(reminderAccount))
                assertEquals(SignOutResult.SignedOut, controller.signOut(snapshot, discardPending = true))
                assertTrue(alarms.active.isEmpty())
                assertTrue(notices.removed >= 1)
                assertNull(accounts.activeAccountDatabase())
                assertFalse(directory.exists())
                assertNull(credentials.data)
            } finally {
                withContext(NonCancellable) {
                    service.close()
                    accounts.close()
                    fixture.close()
                    controller.close()
                    val field = SessionController::class.java.getDeclaredField("scope").apply { isAccessible = true }
                    (field.get(controller) as CoroutineScope).coroutineContext.job.cancelAndJoin()
                    check(root.deleteRecursively())
                }
            }
        }
}

suspend fun copyReminderPlan(
    from: AccountDatabase,
    to: AccountDatabase,
) {
    val trip = TripRepository(from).trips().single()
    val plan = checkNotNull(TripRepository(from).plan(trip.id, "hash"))
    TripRepository(to).upsert(trip)
    TripRepository(to).store(plan, emptyList())
    SettingsRepository(
        to,
    ).upsert(
        SettingsRecord("reminders", """{"light":true,"avoid":true,"caffeine":true,"winddown":true,"leadMin":30}"""),
    )
}
