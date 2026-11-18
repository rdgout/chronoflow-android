package net.chronoflow.core.testing.sync

import kotlinx.coroutines.CompletableDeferred

class ManualSyncScheduler {
    private val sleeps = linkedMapOf<Any, CompletableDeferred<Unit>>()
    private var arrival = CompletableDeferred<Unit>()
    val requestedDelays = mutableListOf<Double>()
    val pendingCount: Int get() = sleeps.size

    suspend fun sleep(seconds: Double) {
        val id = Any()
        val pending = CompletableDeferred<Unit>()
        sleeps[id] = pending
        requestedDelays += seconds
        arrival.complete(Unit)
        try {
            pending.await()
        } finally {
            sleeps.remove(id)
        }
    }

    suspend fun waitForSleep() {
        if (sleeps.isEmpty()) {
            arrival = CompletableDeferred()
            arrival.await()
        }
    }

    fun advance() {
        val pending = sleeps.values.toList()
        sleeps.clear()
        pending.forEach { it.complete(Unit) }
    }
}
