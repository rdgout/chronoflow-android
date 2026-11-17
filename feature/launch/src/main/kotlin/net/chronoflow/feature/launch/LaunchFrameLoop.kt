package net.chronoflow.feature.launch

import kotlinx.coroutines.Deferred

internal suspend fun runLaunchOpening(
    firstDraw: Deferred<Long>,
    nanoTime: () -> Long,
    nextFrameNanos: suspend () -> Long,
    settledNanos: Long,
    onElapsed: (Long) -> Unit,
) {
    val t0 = firstDraw.await()
    var elapsed = 0L
    while (elapsed < settledNanos) {
        nextFrameNanos()
        elapsed = (nanoTime() - t0).coerceAtLeast(0L)
        onElapsed(elapsed)
    }
}
