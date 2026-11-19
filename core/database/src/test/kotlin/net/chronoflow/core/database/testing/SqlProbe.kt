package net.chronoflow.core.database.testing

import app.cash.sqldelight.db.QueryResult
import net.chronoflow.core.database.AccountSql

fun AccountSql.long(sql: String): Long? =
    driver
        .executeQuery(null, sql, { c ->
            QueryResult.Value(if (c.next().value) c.getLong(0) else null)
        }, 0)
        .value

fun AccountSql.string(sql: String): String? =
    driver
        .executeQuery(null, sql, { c ->
            QueryResult.Value(if (c.next().value) c.getString(0) else null)
        }, 0)
        .value

fun AccountSql.exec(sql: String) {
    driver.execute(null, sql, 0)
}
