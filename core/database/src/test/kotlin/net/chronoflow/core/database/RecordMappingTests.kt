package net.chronoflow.core.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.database.store.deleteOutboxRow
import net.chronoflow.core.database.store.deleteSetting
import net.chronoflow.core.database.store.deleteTrip
import net.chronoflow.core.database.store.fetchAccount
import net.chronoflow.core.database.store.fetchOutboxRow
import net.chronoflow.core.database.store.fetchPendingOutbox
import net.chronoflow.core.database.store.fetchSetting
import net.chronoflow.core.database.store.fetchSettings
import net.chronoflow.core.database.store.fetchTrip
import net.chronoflow.core.database.store.fetchTrips
import net.chronoflow.core.database.store.saveAccount
import net.chronoflow.core.database.store.saveOutboxRow
import net.chronoflow.core.database.store.saveSetting
import net.chronoflow.core.database.store.saveTrip
import net.chronoflow.core.database.testing.JdbcAccountDriverFactory
import net.chronoflow.core.database.testing.TestRoots
import net.chronoflow.core.database.testing.accountA
import net.chronoflow.core.database.testing.exec
import net.chronoflow.core.database.testing.long
import net.chronoflow.core.database.testing.string
import net.chronoflow.core.model.store.AccountRecord
import net.chronoflow.core.model.store.JSONDocument
import net.chronoflow.core.model.store.OutboxRecord
import net.chronoflow.core.model.store.SettingsRecord
import net.chronoflow.core.model.store.TripRecord
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.fail
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class RecordMappingTests {
    private val root = TestRoots.temporaryStoreRoot()
    private val db = AccountDatabase(root, driverFactory = JdbcAccountDriverFactory(), io = Dispatchers.IO)

    @After fun cleanup() {
        root.deleteRecursively()
    }

    private suspend fun opened(body: suspend () -> Unit) {
        db.open(accountA)
        try {
            body()
        } finally {
            db.close()
        }
    }

    @Test fun invalidJSONCannotEnterARecord() =
        runTest(timeout = 60.seconds) {
            opened {
                assertThrows(IllegalArgumentException::class.java) { JSONDocument.requireValid("{bad") }
                assertEquals("[null,1,{}]", JSONDocument.requireValid("[null,1,{}]"))
                try {
                    db.write { it.saveSetting(SettingsRecord("s", "{bad")) }
                    fail()
                } catch (_: IllegalArgumentException) {
                }
                assertNull(db.read { it.fetchSetting("s") })
            }
        }

    @Test fun fourRecordsRoundTripUnknownJsonAndNulls() =
        runTest(timeout = 60.seconds) {
            opened {
                val json = """{"future":{"big":123456789012345678901234567890,"list":[null,true,"é"]},"n":1.000}"""
                val account = AccountRecord(accountA, "Dutch ' name", "a@example.test", hasVerifiedEmail = false)
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
                val setting = SettingsRecord("futureSection", json)
                val outbox = OutboxRecord("id", "futureKind", json, "now", 2, "offline")
                db.write {
                    it.saveAccount(account)
                    it.saveTrip(trip)
                    it.saveSetting(setting)
                    it.saveOutboxRow(outbox)
                }
                db.read {
                    assertEquals(account, it.fetchAccount(accountA))
                    assertEquals(trip, it.fetchTrip("trip'"))
                    assertEquals(setting, it.fetchSetting("futureSection"))
                    assertEquals(outbox, it.fetchOutboxRow("id"))
                    assertEquals(json, it.string("SELECT card FROM trips"))
                }
                val nullable = TripRecord("nullable", card = "null", departureUtc = "later")
                db.write { it.saveTrip(nullable) }
                assertEquals(nullable, db.read { it.fetchTrip("nullable") })
            }
        }

    @Test fun outboxOrdersByTimestampThenIdAndBindsKeys() =
        runTest(timeout = 60.seconds) {
            opened {
                db.write {
                    it.saveOutboxRow(OutboxRecord("b", "k", "{}", "1"))
                    it.saveOutboxRow(OutboxRecord("a", "k", "{}", "1"))
                    it.saveOutboxRow(OutboxRecord("c", "k", "{}", "0"))
                }
                assertEquals(listOf("c", "a", "b"), db.read { it.fetchPendingOutbox().map { row -> row.id } })
                db.write { it.deleteOutboxRow("' OR 1=1 --") }
                assertEquals(3, db.read { it.fetchPendingOutbox().size })
                db.write { it.deleteOutboxRow("a") }
                assertEquals(listOf("c", "b"), db.read { it.fetchPendingOutbox().map { row -> row.id } })
            }
        }

    @Test fun saveNeverCascades() =
        runTest(timeout = 60.seconds) {
            opened {
                db.write {
                    it.saveTrip(TripRecord("trip", "{}", departureUtc = "date"))
                    it.exec(
                        (
                            "INSERT INTO plan_versions (trip_id,input_hash,detail,received_at) VALUES " +
                                "('trip','hash','{}','date')"
                        ),
                    )
                    it.exec("INSERT INTO check_marks VALUES ('trip','hash','sleep','date',0,'done','date')")
                    it.saveTrip(TripRecord("trip", "{\"new\":true}", departureUtc = "date"))
                }
                assertEquals(1L, db.read { it.long("SELECT count(*) FROM plan_versions") })
                assertEquals(1L, db.read { it.long("SELECT count(*) FROM check_marks") })
            }
        }

    @Test fun saveUpdatesInPlace() =
        runTest(timeout = 60.seconds) {
            opened {
                val account = AccountRecord(accountA, "first", "email")
                val trip = TripRecord("trip", "{}", departureUtc = "date")
                db.write {
                    it.saveAccount(account)
                    it.saveAccount(account.copy(name = "second"))
                    it.saveTrip(trip)
                    it.saveTrip(trip.copy(card = "null"))
                    it.saveSetting(SettingsRecord("s", "1"))
                    it.saveSetting(SettingsRecord("s", "2"))
                    it.saveOutboxRow(OutboxRecord("o", "k", "{}", "now"))
                    it.saveOutboxRow(OutboxRecord("o", "other", "null", "later"))
                }
                db.read {
                    assertEquals(account.copy(name = "second"), it.fetchAccount(accountA))
                    assertEquals(listOf(trip.copy(card = "null")), it.fetchTrips())
                    assertEquals(listOf(SettingsRecord("s", "2")), it.fetchSettings())
                    assertEquals(listOf(OutboxRecord("o", "other", "null", "later")), it.fetchPendingOutbox())
                }
                db.write {
                    it.deleteTrip("trip")
                    it.deleteSetting("s")
                }
                db.read {
                    assertNull(it.fetchTrip("trip"))
                    assertNull(it.fetchSetting("s"))
                }
            }
        }
}
