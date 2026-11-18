package net.chronoflow.core.testing.sync

import kotlinx.coroutines.CompletableDeferred

class TestGate {
    private val arrived = CompletableDeferred<Unit>()
    private val released = CompletableDeferred<Unit>()

    suspend fun block() {
        arrived.complete(Unit)
        released.await()
    }

    suspend fun waitUntilBlocked() {
        arrived.await()
    }

    fun release() {
        released.complete(Unit)
    }
}
