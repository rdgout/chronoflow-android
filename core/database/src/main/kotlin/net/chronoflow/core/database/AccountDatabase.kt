package net.chronoflow.core.database

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.session.StorageNamespace
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class AccountDatabase(
    private val root: File,
    val namespace: StorageNamespace = StorageNamespace.Api,
    private val driverFactory: AccountDriverFactory,
    private val io: CoroutineDispatcher,
) {
    private val writer = io.limitedParallelism(1)
    private val lifecycle = ReentrantReadWriteLock()

    @Volatile var accountID: AccountID? = null
        private set
    private var boundID: AccountID? = null
    private var current: AccountSql? = null

    fun directory(id: AccountID): File = File(root, "${namespace.directoryName}/Accounts/${id.rawValue}")

    fun file(id: AccountID): File = File(directory(id), "account-${id.rawValue}.db")

    suspend fun open(accountID: String) =
        withContext(writer) {
            val id =
                try {
                    AccountID.of(accountID)
                } catch (
                    _: AccountID.InvalidUUID,
                ) {
                    throw StoreFailure.InvalidAccountID
                }
            lifecycle.write {
                if (boundID != null && boundID != id) throw StoreFailure.AlreadyOpen
                if (current != null) return@write
                val directory = directory(id)
                if (!directory.mkdirs() && !directory.isDirectory) throw StoreFailure.StorageAttributes()
                val driver = driverFactory.open(file(id))
                current = AccountSql(AccountDb(driver), driver)
                this@AccountDatabase.accountID = id
                boundID = id
            }
        }

    suspend fun <T> read(body: (AccountSql) -> T): T =
        withContext(io) {
            lifecycle.read { body(current ?: throw StoreFailure.Closed) }
        }

    suspend fun <T> write(body: (AccountSql) -> T): T =
        withContext(writer) {
            lifecycle.read {
                val sql = current ?: throw StoreFailure.Closed
                sql.queries.transactionWithResult { body(sql) }
            }
        }

    suspend fun close() =
        withContext(io) {
            lifecycle.write {
                current?.driver?.close()
                current = null
                accountID = null
            }
        }
}
