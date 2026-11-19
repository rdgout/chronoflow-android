package net.chronoflow.core.model.session

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import net.chronoflow.core.model.AccountID
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class SyncEpoch private constructor(
    val accountID: AccountID,
    val generation: Long,
    private val parent: SyncEpoch?,
) {
    constructor(accountID: AccountID, generation: Long) : this(accountID, generation, null)

    private val lock = ReentrantLock()
    private var valid = true

    fun child(): SyncEpoch = SyncEpoch(accountID, generation, this)

    fun invalidate() {
        lock.withLock { valid = false }
    }

    val isValid: Boolean get() = (parent?.isValid ?: true) && lock.withLock { valid }

    suspend fun check() {
        currentCoroutineContext().ensureActive()
        whileValid { }
    }

    // Hold validity through SQLite's commit so detach cannot race response application.
    fun <T> whileValid(operation: () -> T): T =
        if (parent != null) parent.whileValid { ownValidity(operation) } else ownValidity(operation)

    private fun <T> ownValidity(operation: () -> T): T =
        lock.withLock {
            if (!valid) throw StaleSessionException()
            operation()
        }
}
