package net.chronoflow.core.model.session

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class SessionMutationPermit {
    private val lock = ReentrantLock()
    private var valid = true

    /** Only the session controller invalidates a permit. Publication waits for an admitted SQLite transaction to commit before revoking the session. */
    fun invalidate() {
        lock.withLock { valid = false }
    }

    fun <T> whileValid(operation: () -> T): T =
        lock.withLock {
            if (!valid) throw StaleSessionException()
            operation()
        }
}
