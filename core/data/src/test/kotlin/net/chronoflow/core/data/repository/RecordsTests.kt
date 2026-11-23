package net.chronoflow.core.data.repository

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.database.store.fetchAccount
import net.chronoflow.core.database.store.fetchCheckMarks
import net.chronoflow.core.database.store.fetchOutboxRow
import net.chronoflow.core.database.store.fetchPlanVersion
import net.chronoflow.core.database.store.fetchSetting
import net.chronoflow.core.database.store.fetchTrip
import net.chronoflow.core.database.store.saveAccount
import net.chronoflow.core.database.store.saveCheckMark
import net.chronoflow.core.database.store.saveOutboxRow
import net.chronoflow.core.database.store.savePlanVersion
import net.chronoflow.core.database.store.saveSetting
import net.chronoflow.core.database.store.saveTrip
import net.chronoflow.core.model.store.AccountRecord
import net.chronoflow.core.model.store.CheckMarkRecord
import net.chronoflow.core.model.store.OutboxRecord
import net.chronoflow.core.model.store.PlanVersionRecord
import net.chronoflow.core.model.store.SettingsRecord
import net.chronoflow.core.model.store.TripRecord
import net.chronoflow.core.testing.sync.syncAccount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class RecordsTests {
    @Test fun allRecordsRoundTripUnknownJSONNullsAndCompositeKeys() =
        runTest(timeout = 60.seconds) {
            val store = RepositoryStore(StandardTestDispatcher(testScheduler))
            val database = store.database
            try {
                val json = """{"future":{"big":123456789012345678901234567890,"list":[null,true,"é"]},"n":1.000}"""
                val account = AccountRecord(syncAccount, "Dutch ' name", "a@example.test", false)
                val trip =
                    TripRecord(
                        "trip'",
                        json,
                        json,
                        json,
                        json,
                        "hash",
                        true,
                        "date",
                        "start",
                        "Europe/Amsterdam",
                        2,
                        "end",
                        "day",
                        "deleted",
                        true,
                    )
                val plan = PlanVersionRecord(trip.id, "hash", json.encodeToByteArray(), "date", "v")
                val mark = CheckMarkRecord(trip.id, "hash", "sleep", "date", 0, "done", "date")
                val setting = SettingsRecord("futureSection", json)
                val outbox = OutboxRecord("id", "futureKind", json, "now", 2, "offline")
                database.write { sql ->
                    sql.saveAccount(account)
                    sql.saveTrip(trip)
                    sql.savePlanVersion(plan)
                    sql.saveCheckMark(mark)
                    sql.saveSetting(setting)
                    sql.saveOutboxRow(outbox)
                }
                database.read { sql ->
                    assertEquals(account, sql.fetchAccount(syncAccount))
                    assertEquals(trip, sql.fetchTrip(trip.id))
                    assertEquals(plan, sql.fetchPlanVersion(trip.id, "hash"))
                    assertEquals(listOf(mark), sql.fetchCheckMarks(trip.id, "hash"))
                    assertEquals(setting, sql.fetchSetting("futureSection"))
                    assertEquals(outbox, sql.fetchOutboxRow("id"))
                    assertEquals(
                        json,
                        sql.queries.tripsQueries
                            .selectTrip(trip.id)
                            .executeAsOne()
                            .card,
                    )
                }
                val other = mark.copy(ordinal = 1, status = "skipped", markedAt = "later")
                TripRepository(database).store(plan, listOf(other))
                assertEquals(listOf(mark, other), TripRepository(database).checks(trip.id, "hash"))
                val nullable = TripRecord("nullable", "null", departureUtc = "later")
                TripRepository(database).upsert(nullable)
                assertTrue(nullable in TripRepository(database).trips())
            } finally {
                store.close()
            }
        }

    @Test fun repositoriesBindKeysAndOrderOutboxByTimestampThenID() =
        runTest(timeout = 60.seconds) {
            val store = RepositoryStore(StandardTestDispatcher(testScheduler))
            try {
                val repository = OutboxRepository(store.database)
                repository.upsert(OutboxRecord("b", "settings", "{}", "1"))
                repository.upsert(OutboxRecord("a", "settings", "{}", "1"))
                repository.upsert(OutboxRecord("c", "settings", "{}", "0"))
                assertEquals(listOf("c", "a", "b"), repository.pending().map { it.id })
                repository.remove("' OR 1=1 --")
                assertEquals(3, repository.pending().size)
                repository.remove("a")
                assertEquals(listOf("c", "b"), repository.pending().map { it.id })
                val settings = SettingsRepository(store.database)
                settings.upsert(SettingsRecord("preferences", "{}"))
                settings.remove("preferences")
                assertTrue(settings.settings().isEmpty())
            } finally {
                store.close()
            }
        }
}
