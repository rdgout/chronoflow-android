package net.chronoflow.core.database

import android.content.Context
import android.database.SQLException
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import java.io.File

class AndroidAccountDriverFactory(
    private val context: Context,
    private val schema: SqlSchema<QueryResult.Value<Unit>> = AccountDb.Schema,
) : AccountDriverFactory {
    override fun open(file: File): SqlDriver {
        val driver = AndroidSqliteDriver(schema, context, file.absolutePath, callback = Callback(schema))
        try {
            driver.checkConfiguration()
            return driver
        } catch (e: RuntimeException) {
            driver.close()
            var cause: Throwable? = e
            while (cause != null) {
                if (cause is StoreFailure.Migration) throw cause
                cause = cause.cause
            }
            throw StoreFailure.Unavailable(e)
        } catch (e: StoreFailure) {
            driver.close()
            throw e
        }
    }

    private class Callback(
        schema: SqlSchema<QueryResult.Value<Unit>>,
    ) : AndroidSqliteDriver.Callback(schema) {
        override fun onConfigure(db: SupportSQLiteDatabase) {
            db.enableWriteAheadLogging()
            db.setForeignKeyConstraintsEnabled(true)
        }

        override fun onCreate(db: SupportSQLiteDatabase) {
            try {
                super.onCreate(db)
            } catch (e: SQLException) {
                throw StoreFailure.Migration(e)
            } catch (e: RuntimeException) {
                throw StoreFailure.Migration(e)
            }
        }

        override fun onUpgrade(
            db: SupportSQLiteDatabase,
            oldVersion: Int,
            newVersion: Int,
        ): Unit = throw StoreFailure.Migration()

        override fun onDowngrade(
            db: SupportSQLiteDatabase,
            oldVersion: Int,
            newVersion: Int,
        ): Unit = throw StoreFailure.Migration()

        // The framework default deletes account data on corruption; recovery must remain explicit.
        override fun onCorruption(db: SupportSQLiteDatabase) = Unit
    }
}
