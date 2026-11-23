package net.chronoflow.core.testing.sync

import kotlinx.coroutines.CompletableDeferred
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class TestGate {
    private val arrived = CompletableDeferred<Unit>()
    private var blocked: Continuation<Unit>? = null

    suspend fun block(): Unit =
        suspendCoroutine { continuation ->
            blocked = continuation
            arrived.complete(Unit)
        }

    suspend fun waitUntilBlocked() {
        arrived.await()
    }

    fun release() {
        val pending = blocked
        blocked = null
        pending?.resume(Unit)
    }
}
