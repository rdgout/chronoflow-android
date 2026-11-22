package net.chronoflow.core.data.reminders

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.database.AccountDatabase
import net.chronoflow.core.database.testing.JdbcAccountDriverFactory
import net.chronoflow.core.database.testing.TestRoots
import net.chronoflow.core.reminders.ReminderLedgerEntry
import net.chronoflow.core.reminders.ReminderRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.sql.SQLException
import java.time.Instant
import kotlin.math.floor
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderStoreTests {
    private val now = Instant.parse("2026-10-14T00:00:00Z")
    private val request =
        ReminderRequest(
            "reminder.b",
            "Title",
            "Body",
            now.plusSeconds(1800),
            "trip",
            "light",
            "hash",
            listOf("seek_light|2026-10-14|1"),
            "01900000-0000-7000-8000-000000000001",
            now.plusSeconds(4800),
        )

    @Test fun rowsRoundTripAndSaveUpdatesInPlace() =
        runTest(timeout = 60.seconds) {
            val root = TestRoots.temporaryStoreRoot()
            val db =
                AccountDatabase(
                    root,
                    driverFactory = JdbcAccountDriverFactory(),
                    io = StandardTestDispatcher(testScheduler),
                )
            db.open(checkNotNull(request.accountID))
            try {
                val original = ScheduledReminderRow.of(request, 7)
                ReminderStore.save(db, original)
                assertEquals(original, ReminderStore.rows(db).single())
                assertEquals(request, ReminderStore.rows(db).single().toRequest())
                assertTrue(original.sameContent(request))
                assertFalse(original.sameContent(request.copy(expiresAt = request.expiresAt.plusSeconds(1))))
                assertFalse(original.sameContent(request.copy(accountID = "other")))
                val updated = original.copy(body = "Updated")
                ReminderStore.save(db, updated)
                assertEquals(listOf(updated), ReminderStore.rows(db))
                ReminderStore.save(db, original.copy(identifier = "reminder.a", requestCode = 8))
                ReminderStore.save(db, original.copy(identifier = "earlier", requestCode = 9, sendAt = now))
                assertEquals(
                    listOf("earlier", "reminder.a", "reminder.b"),
                    ReminderStore.rows(db).map { it.identifier },
                )
                try {
                    ReminderStore.save(db, original.copy(identifier = "duplicate"))
                    fail("request codes must be unique")
                } catch (_: SQLException) {
                    assertEquals(3, ReminderStore.rows(db).size)
                }
                ReminderStore.delete(db, "earlier")
                assertEquals(2, ReminderStore.rows(db).size)
                ReminderStore.deleteAll(db)
                assertTrue(ReminderStore.rows(db).isEmpty())
            } finally {
                withContext(NonCancellable) {
                    db.close()
                    check(root.deleteRecursively())
                }
            }
        }

    @Test fun ledgerRoundTripFloorsMilliseconds() =
        runTest(timeout = 60.seconds) {
            val root = TestRoots.temporaryStoreRoot()
            val db =
                AccountDatabase(
                    root,
                    driverFactory = JdbcAccountDriverFactory(),
                    io = StandardTestDispatcher(testScheduler),
                )
            db.open(checkNotNull(request.accountID))
            try {
                val entry = ReminderLedgerEntry("trip", "hash", 1.7928e12 + 0.9, request.memberKeys)
                ReminderStore.replaceLedger(db, listOf(entry))
                assertEquals(listOf(entry.copy(sendAtMs = floor(entry.sendAtMs))), ReminderStore.ledger(db))
                val replacement = entry.copy(inputHash = "new", sendAtMs = -1.1)
                ReminderStore.replaceLedger(db, listOf(replacement))
                assertEquals(listOf(replacement.copy(sendAtMs = -2.0)), ReminderStore.ledger(db))
                db.write { it.queries.reminderLedgerQueries.insertReminderLedger("trip", "bad", 1, "[1]") }
                assertEquals(1, ReminderStore.ledger(db).size)
                ReminderStore.clearLedger(db)
                assertTrue(ReminderStore.ledger(db).isEmpty())
            } finally {
                withContext(NonCancellable) {
                    db.close()
                    check(root.deleteRecursively())
                }
            }
        }

    @Test fun reminderWritesDoNotWakeInputObservation() =
        runTest(timeout = 60.seconds) {
            val root = TestRoots.temporaryStoreRoot()
            val db =
                AccountDatabase(
                    root,
                    driverFactory = JdbcAccountDriverFactory(),
                    io = StandardTestDispatcher(testScheduler),
                )
            db.open(checkNotNull(request.accountID))
            try {
                db.observe("settings", "trips", "plan_versions", "check_marks") { 0 }.test {
                    assertEquals(0, awaitItem())
                    ReminderStore.save(db, ScheduledReminderRow.of(request, 7))
                    ReminderStore.replaceLedger(
                        db,
                        listOf(ReminderLedgerEntry("trip", "hash", 1.0, request.memberKeys)),
                    )
                    runCurrent()
                    expectNoEvents()
                    db.write { it.queries.settingsQueries.insertSetting("reminders", "{}") }
                    assertEquals(0, awaitItem())
                    cancelAndIgnoreRemainingEvents()
                }
            } finally {
                withContext(NonCancellable) {
                    db.close()
                    check(root.deleteRecursively())
                }
            }
        }
}
