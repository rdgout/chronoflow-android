package net.chronoflow.core.testing.sync

import kotlinx.coroutines.CoroutineDispatcher
import kotlin.coroutines.CoroutineContext

class WriteBarrier(
    private val delegate: CoroutineDispatcher,
) : CoroutineDispatcher() {
    private val queued = ArrayDeque<Pair<CoroutineContext, Runnable>>()
    private var blocked = false

    fun block() {
        blocked = true
    }

    val pendingCount: Int get() = queued.size

    override fun dispatch(
        context: CoroutineContext,
        block: Runnable,
    ) {
        if (blocked) queued.addLast(context to block) else delegate.dispatch(context, block)
    }

    fun release() {
        blocked = false
        while (queued.isNotEmpty()) {
            val (context, block) = queued.removeFirst()
            delegate.dispatch(context, block)
        }
    }
}
