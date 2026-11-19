package net.chronoflow.core.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.chronoflow.core.database.testing.JdbcAccountDriverFactory
import net.chronoflow.core.database.testing.TestRoots
import net.chronoflow.core.database.testing.accountA
import net.chronoflow.core.database.testing.exec
import net.chronoflow.core.database.testing.long
import net.chronoflow.core.model.api.ApiJson
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class SchemaV1Tests {
    private val root = TestRoots.temporaryStoreRoot()
    private val db = AccountDatabase(root, driverFactory = JdbcAccountDriverFactory(), io = Dispatchers.IO)

    @After fun cleanup() {
        root.deleteRecursively()
    }

    @Test fun matchesFinalRNSchemaAndReopensIdempotently() =
        runTest(timeout = 60.seconds) {
            val fixture =
                ApiJson.response
                    .parseToJsonElement(
                        javaClass.getResource("/rn-schema-v1.json")!!.readText(),
                    ).jsonObject
            repeat(2) {
                db.open(accountA)
                try {
                    for ((table, expected) in fixture) {
                        val columns = db.read { sql -> sql.driver.rows("PRAGMA table_info($table)", 6) }
                        val normalized =
                            columns
                                .map { row ->
                                    listOf(row[1], row[2], row[3], normalize(row[4]), row[5])
                                }.sortedBy { it[0] }
                        val wanted =
                            expected.jsonObject
                                .getValue("columns")
                                .jsonArray
                                .map { col ->
                                    val obj = col.jsonObject
                                    listOf(
                                        "name",
                                        "type",
                                        "notnull",
                                        "default",
                                        "pk",
                                    ).map { normalize(obj.getValue(it).jsonPrimitive.content) }
                                }.sortedBy { it[0] }
                        assertEquals(table, wanted, normalized)
                        val keys =
                            db
                                .read { sql -> sql.driver.rows("PRAGMA foreign_key_list($table)", 8) }
                                .map { row -> listOf(row[2], row[3], row[4], row[6]) }
                        val wantedKeys =
                            expected.jsonObject.getValue("foreignKeys").jsonArray.map { key ->
                                listOf(
                                    "table",
                                    "from",
                                    "to",
                                    "on_delete",
                                ).map {
                                    key.jsonObject
                                        .getValue(it)
                                        .jsonPrimitive.content
                                }
                            }
                        assertEquals(table, wantedKeys, keys)
                    }
                    assertEquals(1L, db.read { it.long("PRAGMA user_version") })
                    val tables =
                        db.read {
                            it.driver
                                .rows("SELECT name FROM sqlite_master WHERE type='table'", 1)
                                .map { row ->
                                    row.single()
                                }.toSet()
                        }
                    assertEquals(fixture.keys + setOf("scheduled_reminder", "reminder_ledger"), tables)
                } finally {
                    db.close()
                }
            }
        }

    @Test fun foreignKeysCascadeAndRollbackRetainsUnrelatedCheckMarks() =
        runTest(timeout = 60.seconds) {
            db.open(accountA)
            try {
                db.write {
                    it.exec("INSERT INTO trips (id,card,departure_utc) VALUES ('trip','{}','date')")
                    it.exec(
                        (
                            "INSERT INTO plan_versions (trip_id,input_hash,detail,received_at) VALUES " +
                                "('trip','hash','{}','date')"
                        ),
                    )
                    it.exec("INSERT INTO check_marks VALUES ('trip','hash','sleep','date',0,'done','date')")
                }
                try {
                    db.write {
                        it.exec("DELETE FROM trips")
                        error("rollback")
                    }
                    fail()
                } catch (
                    _: IllegalStateException,
                ) {
                }
                assertEquals(1L, db.read { it.long("SELECT count(*) FROM plan_versions") })
                db.write { it.exec("DELETE FROM trips") }
                assertEquals(0L, db.read { it.long("SELECT count(*) FROM plan_versions") })
                assertEquals(1L, db.read { it.long("SELECT count(*) FROM check_marks") })
            } finally {
                db.close()
            }
        }

    private fun normalize(value: String?): String? =
        when (val v = value?.trim('\'')) {
            "null" -> null
            "true" -> "1"
            "false" -> "0"
            else -> v
        }
}
