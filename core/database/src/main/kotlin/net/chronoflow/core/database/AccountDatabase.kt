package net.chronoflow.core.database

import app.cash.sqldelight.Query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.session.SessionMutationPermit
import net.chronoflow.core.model.session.StorageNamespace
import net.chronoflow.core.model.session.SyncEpoch
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
    private val observers = mutableSetOf<ProducerScope<Unit>>()
    val observationCount: Int get() = synchronized(observers) { observers.size }

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

    suspend fun <T> write(
        permit: SessionMutationPermit?,
        body: (AccountSql) -> T,
    ): T {
        if (permit == null) return write(body)
        return withContext(writer) {
            lifecycle.read {
                val sql = current ?: throw StoreFailure.Closed
                permit.whileValid { sql.queries.transactionWithResult { body(sql) } }
            }
        }
    }

    suspend fun validate(epoch: SyncEpoch) {
        epoch.check()
        lifecycle.read {
            if (accountID != epoch.accountID) throw StoreFailure.InvalidAccountID
            if (current == null) throw StoreFailure.Closed
        }
    }

    suspend fun <T> write(
        epoch: SyncEpoch,
        permit: SessionMutationPermit? = null,
        body: (AccountSql) -> T,
    ): T =
        try {
            validate(epoch)
            withContext(writer) {
                lifecycle.read {
                    val sql = current ?: throw StoreFailure.Closed
                    epoch.whileValid {
                        fun transaction(): T = sql.queries.transactionWithResult { body(sql) }
                        if (permit != null) permit.whileValid { transaction() } else transaction()
                    }
                }
            }
        } catch (e: CancellationException) {
            if (!currentCoroutineContext().isActive) epoch.invalidate()
            throw e
        }

    private class Box<T>(
        val value: T,
    )

    fun <T> observe(
        vararg tables: String,
        fetch: (AccountSql) -> T,
    ): Flow<T> {
        lifecycle.read { if (current == null) throw StoreFailure.Closed }
        return callbackFlow {
            val listener = Query.Listener { trySend(Unit) }
            val observed =
                withContext(io) {
                    lifecycle.read {
                        current?.also { sql ->
                            synchronized(observers) { observers.add(this@callbackFlow) }
                            sql.driver.addListener(*tables, listener = listener)
                            trySend(Unit)
                        }
                    }
                }
            if (observed == null) close()
            awaitClose {
                observed?.driver?.removeListener(*tables, listener = listener)
                synchronized(observers) { observers.remove(this@callbackFlow) }
            }
        }.buffer(Channel.CONFLATED)
            .mapNotNull {
                withContext(io) { lifecycle.read { current?.let { Box(fetch(it)) } } }
            }.map { it.value }
    }

    suspend fun close() =
        withContext(io) {
            lifecycle.write {
                synchronized(observers) {
                    observers.forEach { it.close() }
                    observers.clear()
                }
                current?.driver?.close()
                current = null
                accountID = null
            }
        }
}
