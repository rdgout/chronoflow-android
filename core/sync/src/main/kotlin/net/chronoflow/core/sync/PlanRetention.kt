package net.chronoflow.core.sync

import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class PlanRetention {
    private val lock = ReentrantLock()
    private val leases = mutableMapOf<UUID, String>()

    fun hold(tripId: String): PlanLease {
        val id = UUID.randomUUID()
        lock.withLock { leases[id] = tripId }
        return PlanLease {
            lock.withLock { leases.remove(id) }
        }
    }

    internal fun clear() = lock.withLock { leases.clear() }

    fun <T> withHeldPlans(action: (Set<String>) -> T): T = lock.withLock { action(leases.values.toSet()) }
}

class PlanLease internal constructor(
    private var releaseAction: (() -> Unit)?,
) {
    private val lock = ReentrantLock()

    fun release() {
        val action = lock.withLock { releaseAction.also { releaseAction = null } }
        action?.invoke()
    }
}
