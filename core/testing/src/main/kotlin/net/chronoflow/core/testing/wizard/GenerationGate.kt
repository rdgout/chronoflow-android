package net.chronoflow.core.testing.wizard

import kotlinx.coroutines.CompletableDeferred

class GenerationGate {
    private val opened = CompletableDeferred<Unit>()

    suspend fun wait() = opened.await()

    fun open() {
        opened.complete(Unit)
    }
}
