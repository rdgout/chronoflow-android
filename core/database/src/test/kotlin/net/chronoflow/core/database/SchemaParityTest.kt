package net.chronoflow.core.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

class SchemaParityTest {
    private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { AccountDb.Schema.create(it) }

    @After fun close() {
        driver.close()
    }

    @Test fun resourceHashesMatchThePin() {
        fun hash(name: String) =
            MessageDigest
                .getInstance(
                    "SHA-256",
                ).digest(javaClass.getResourceAsStream("/$name")!!.use { it.readBytes() })
                .joinToString("") { "%02x".format(it) }
        assertEquals("6014a032b63a97f72d24858d4c08f1fcb496bb0a44dcd70af25b70688ee4b583", hash("ios-schema-v1.sql"))
        assertEquals("937c93693e8dc17c60572404a6cb0825860bb6aafb9840b3bde0ee9b2e8dd54a", hash("rn-schema-v1.json"))
    }

    @Test fun iosTablesMatchVerbatim() {
        fun normalize(sql: String) =
            sql
                .trim()
                .trimEnd(';')
                .lowercase()
                .replace(Regex("\\s+"), " ")
                .replace(
                    Regex("\\s*([(),])\\s*"),
                    "$1",
                ).replace("default true", "default 1")
                .replace("default false", "default 0")
        val actual =
            driver
                .rows(
                    "SELECT name, sql FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'",
                    2,
                ).associate {
                    it[0]!! to
                        it[1]!!
                }
        val expected =
            javaClass
                .getResource("/ios-schema-v1.sql")!!
                .readText()
                .split(';')
                .filter { it.isNotBlank() }
        assertEquals(7, expected.size)
        for (sql in expected) {
            val table = Regex("CREATE TABLE (\\w+)").find(sql)!!.groupValues[1]
            assertEquals(table, normalize(sql), normalize(actual.getValue(table)))
        }
    }

    @Test fun onlyTheDeclaredExtrasExist() {
        assertEquals(
            setOf(
                "account",
                "trips",
                "plan_versions",
                "check_marks",
                "settings",
                "drafts",
                "outbox",
                "scheduled_reminder",
                "reminder_ledger",
            ),
            driver
                .rows("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'", 1)
                .map {
                    it.single()
                }.toSet(),
        )
        assertEquals(
            setOf("outbox_created_at_id", "trips_deleted_at", "reminder_ledger_send_at_ms"),
            driver
                .rows(
                    "SELECT name FROM sqlite_master WHERE type='index' AND name NOT LIKE 'sqlite_autoindex%'",
                    1,
                ).map {
                    it.single()
                }.toSet(),
        )
    }

    @Test fun booleanDefaultsAreIntegers() {
        fun defaults(table: String) = driver.rows("PRAGMA table_info($table)", 6).associate { it[1] to it[4] }
        assertEquals("1", defaults("account")["has_verified_email"])
        assertEquals("0", defaults("account")["walkthrough_seen"])
        assertEquals("1", defaults("drafts")["dirty"])
        assertEquals("0", defaults("trips")["keep_offline"])
        assertEquals("0", defaults("trips")["deleted_locally"])
        driver.execute(null, "INSERT INTO account (id,name,email) VALUES ('a','n','e')", 0)
        driver.executeQuery(
            null,
            "SELECT has_verified_email, walkthrough_seen, " +
                "typeof(has_verified_email), typeof(walkthrough_seen) FROM account",
            { c ->
                assertTrue(c.next().value)
                assertEquals(1L, c.getLong(0))
                assertEquals(0L, c.getLong(1))
                assertEquals("integer", c.getString(2))
                assertEquals("integer", c.getString(3))
                app.cash.sqldelight.db.QueryResult
                    .Value(Unit)
            },
            0,
        )
    }
}
