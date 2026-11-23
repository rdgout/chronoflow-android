package net.chronoflow.feature.launch

import kotlinx.coroutines.Deferred

internal suspend fun runLaunchOpening(
    firstDraw: Deferred<Long>,
    nanoTime: () -> Long,
    nextFrameNanos: suspend () -> Long,
    settledNanos: Long,
    planProvider: (() -> HandOffPlan)? = null,
    awaitRevision: (suspend (Long) -> Unit)? = null,
    onExit: (Long) -> Unit = {},
    onElapsed: (Long) -> Unit,
) {
    val t0 = firstDraw.await()
    var elapsed = 0L
    while (true) {
        val plan = planProvider?.invoke()
        val settled = plan?.sampler?.settledAfterSeconds?.let { (it * 1e9).toLong() } ?: settledNanos
        val exit = plan?.exitEndSeconds?.let { (it * 1e9).toLong() }
        if (elapsed < settled || exit != null) {
            nextFrameNanos()
            elapsed = (nanoTime() - t0).coerceAtLeast(0L)
            onElapsed(elapsed)
            if (exit != null && elapsed >= exit) {
                onExit(elapsed)
                return
            }
        } else {
            if (awaitRevision == null || plan == null) return
            awaitRevision(plan.revision)
        }
    }
}
