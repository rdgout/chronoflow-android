package net.chronoflow.core.plan

import kotlinx.coroutines.CoroutineDispatcher
import kotlin.coroutines.CoroutineContext

class CountingDispatcher(
    private val delegate: CoroutineDispatcher,
) : CoroutineDispatcher() {
    @Volatile var dispatches = 0

    @Volatile var running = false

    override fun dispatch(
        context: CoroutineContext,
        block: Runnable,
    ) {
        dispatches++
        delegate.dispatch(context) {
            running = true
            try {
                block.run()
            } finally {
                running = false
            }
        }
    }
}
