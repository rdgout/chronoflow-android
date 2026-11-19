package net.chronoflow.core.database

import app.cash.sqldelight.db.SqlDriver

class AccountSql internal constructor(
    val queries: AccountDb,
    val driver: SqlDriver,
)
