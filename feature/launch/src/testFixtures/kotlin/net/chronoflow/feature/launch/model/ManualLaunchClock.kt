package net.chronoflow.feature.launch.model

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.time.Duration

class ManualLaunchClock : LaunchClock {
    override var now: Duration = Duration.ZERO
        private set
    private val sleepers = mutableMapOf<CancellableContinuation<Unit>, Duration>()
    val pendingSleeps: Int get() = sleepers.size

    fun advance(to: Duration) {
        require(to >= now)
        now = to
        val due = sleepers.filterValues { it <= to }.keys
        due.forEach {
            sleepers.remove(it)
            it.resume(Unit)
        }
    }

    override suspend fun sleepUntil(deadline: Duration) {
        currentCoroutineContext().ensureActive()
        if (deadline <= now) return
        suspendCancellableCoroutine { continuation ->
            sleepers[continuation] = deadline
            continuation.invokeOnCancellation { sleepers.remove(continuation) }
        }
        currentCoroutineContext().ensureActive()
    }
}
