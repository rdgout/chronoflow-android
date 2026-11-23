package net.chronoflow.core.database.testing

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import net.chronoflow.core.database.AccountDb
import net.chronoflow.core.database.AccountDriverFactory
import net.chronoflow.core.database.checkConfiguration
import net.chronoflow.core.model.store.StoreFailure
import java.io.File
import java.sql.SQLException
import java.util.Properties

class JdbcAccountDriverFactory(
    private val schema: SqlSchema<QueryResult.Value<Unit>> = AccountDb.Schema,
) : AccountDriverFactory {
    override fun open(file: File): SqlDriver {
        val driver =
            JdbcSqliteDriver(
                "jdbc:sqlite:${file.absolutePath}",
                Properties().apply {
                    put("foreign_keys", "true")
                    put("journal_mode", "WAL")
                },
            )
        try {
            driver.checkConfiguration()
            val version =
                driver
                    .executeQuery(null, "PRAGMA user_version", { c ->
                        check(c.next().value)
                        QueryResult.Value(c.getLong(0))
                    }, 0)
                    .value
            if (version == 0L) {
                try {
                    AccountDb(driver).transaction {
                        schema.create(driver).value
                        driver.execute(null, "PRAGMA user_version = 1", 0)
                    }
                } catch (e: RuntimeException) {
                    throw StoreFailure.Migration(e)
                }
            } else if (version != 1L) {
                throw StoreFailure.Migration()
            }
            return driver
        } catch (e: SQLException) {
            driver.close()
            throw StoreFailure.Unavailable(e)
        } catch (e: StoreFailure) {
            driver.close()
            throw e
        }
    }
}
