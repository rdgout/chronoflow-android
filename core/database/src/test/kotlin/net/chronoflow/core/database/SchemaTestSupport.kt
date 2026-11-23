package net.chronoflow.core.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import java.io.File

internal fun SqlDriver.rows(
    sql: String,
    columns: Int,
): List<List<String?>> =
    executeQuery(null, sql, { cursor ->
        val result = mutableListOf<List<String?>>()
        while (cursor.next().value) result += List(columns) { cursor.getString(it) }
        QueryResult.Value(result)
    }, 0).value

internal fun projectRoot(): File {
    var dir: File? = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
    while (dir != null) {
        if (File(dir, "gradlew").isFile && File(dir, "settings.gradle.kts").isFile) return dir
        dir = dir.parentFile
    }
    error("project root not found")
}
