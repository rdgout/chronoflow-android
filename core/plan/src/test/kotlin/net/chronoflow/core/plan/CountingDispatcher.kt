package net.chronoflow.core.plan

import kotlinx.coroutines.CoroutineDispatcher
import kotlin.coroutines.CoroutineContext

class CountingDispatcher(
    private val delegate: CoroutineDispatcher,
) : CoroutineDispatcher() {
    @Volatile var dispatches = 0

    override fun dispatch(
        context: CoroutineContext,
        block: Runnable,
    ) {
        dispatches++
        delegate.dispatch(context, block)
    }
}
