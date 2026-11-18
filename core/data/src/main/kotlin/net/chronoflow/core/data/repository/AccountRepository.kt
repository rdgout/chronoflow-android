package net.chronoflow.core.data.repository

import kotlinx.coroutines.flow.Flow
import net.chronoflow.core.database.AccountDatabase
import net.chronoflow.core.database.store.fetchAccount
import net.chronoflow.core.database.store.saveAccount
import net.chronoflow.core.model.store.AccountRecord
import net.chronoflow.core.model.store.StoreFailure

class AccountRepository(
    val database: AccountDatabase,
) {
    private fun activeID(): String = database.accountID?.rawValue ?: throw StoreFailure.Closed

    suspend fun account(): AccountRecord? {
        val id = activeID()
        return database.read { it.fetchAccount(id) }
    }

    suspend fun upsert(record: AccountRecord) {
        if (record.id != activeID()) throw StoreFailure.InvalidAccountID
        database.write { it.saveAccount(record) }
    }

    fun values(): Flow<AccountRecord?> {
        val id = activeID()
        return database.observe("account") { it.fetchAccount(id) }
    }
}
