package net.chronoflow.core.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Drafts.sq has no foreign-key children, so replacing its recovery row is safe. */
class SqlSafetyTest {
    private val forbidden =
        listOf(
            "OR\\s+REPLACE",
            "\\bREPLACE\\s+INTO",
            "ON\\s+CONFLICT",
            "\\bRETURNING\\b",
            "DEFAULT\\s+(TRUE|FALSE)",
            "\\bOVER\\s*\\(",
        ).map { Regex(it, RegexOption.IGNORE_CASE) }

    private fun safe(
        sql: String,
        fileName: String = "Other.sq",
    ) = forbidden.withIndex().none { (index, rule) ->
        !(fileName == "Drafts.sq" && index < 2) && rule.containsMatchIn(sql)
    }

    @Test fun noSqlNewerThanApi29() {
        val files =
            File(projectRoot(), "core/database/src/main/sqldelight")
                .walkTopDown()
                .filter {
                    it.extension == "sq"
                }.toList()
        assertEquals(9, files.size)
        files.forEach { assertTrue(it.name, safe(it.readText(), it.name)) }
    }

    @Test fun replaceIsAllowedOnlyForDrafts() {
        assertTrue(safe("INSERT OR REPLACE INTO drafts", "Drafts.sq"))
        assertTrue(safe("REPLACE INTO drafts", "Drafts.sq"))
        assertFalse(safe("INSERT OR REPLACE INTO trips", "Trips.sq"))
        assertFalse(safe("REPLACE INTO trips", "Trips.sq"))
        assertFalse(safe("ON CONFLICT", "Drafts.sq"))
        assertFalse(safe("RETURNING id", "Drafts.sq"))
    }

    @Test fun scannerRejectsPlantedViolations() {
        assertFalse(safe("INSERT OR REPLACE"))
        assertFalse(safe("REPLACE INTO t"))
        assertFalse(safe("ON CONFLICT"))
        assertFalse(safe("RETURNING id"))
        assertFalse(safe("DEFAULT true"))
        assertFalse(safe("DEFAULT false"))
        assertFalse(safe("OVER ("))
    }
}
