package net.chronoflow.core.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import java.io.File

/** Opens, configures and migrates without deleting or replacing an existing database. Caller must run on IO. */
fun interface AccountDriverFactory {
    fun open(file: File): SqlDriver
}

internal fun SqlDriver.checkConfiguration() {
    val foreignKeys =
        executeQuery(null, "PRAGMA foreign_keys", { c ->
            QueryResult.Value(if (c.next().value) c.getLong(0) else null)
        }, 0).value
    if (foreignKeys != 1L) throw StoreFailure.Unavailable()
    val journalMode =
        executeQuery(null, "PRAGMA journal_mode", { c ->
            QueryResult.Value(if (c.next().value) c.getString(0) else null)
        }, 0).value
    if (journalMode != "wal") throw StoreFailure.Migration()
}
