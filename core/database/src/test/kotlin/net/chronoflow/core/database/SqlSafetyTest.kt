package net.chronoflow.core.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** 9b may add INSERT OR REPLACE to Drafts.sq, which has no foreign-key children. */
class SqlSafetyTest {
    private val forbidden =
        listOf(
            // 9b: exclude Drafts.sq from the REPLACE checks when its queries land.
            "OR\\s+REPLACE",
            "\\bREPLACE\\s+INTO",
            "ON\\s+CONFLICT",
            "\\bRETURNING\\b",
            "DEFAULT\\s+(TRUE|FALSE)",
            "\\bOVER\\s*\\(",
        ).map { Regex(it, RegexOption.IGNORE_CASE) }

    private fun safe(sql: String) = forbidden.none { it.containsMatchIn(sql) }

    @Test fun noSqlNewerThanApi29() {
        val files =
            File(projectRoot(), "core/database/src/main/sqldelight")
                .walkTopDown()
                .filter {
                    it.extension == "sq"
                }.toList()
        assertEquals(9, files.size)
        files.forEach { assertTrue(it.name, safe(it.readText())) }
    }

    @Test fun scannerRejectsPlantedViolations() {
        listOf(
            "INSERT OR REPLACE",
            "REPLACE INTO t",
            "ON CONFLICT",
            "RETURNING id",
            "DEFAULT true",
            "DEFAULT false",
            "OVER (",
        ).forEach {
            assertFalse(it, safe(it))
        }
    }
}
