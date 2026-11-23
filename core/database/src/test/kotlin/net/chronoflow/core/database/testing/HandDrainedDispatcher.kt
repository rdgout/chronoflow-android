package net.chronoflow.core.database.testing

import kotlinx.coroutines.CoroutineDispatcher
import java.util.ArrayDeque
import kotlin.coroutines.CoroutineContext

class HandDrainedDispatcher : CoroutineDispatcher() {
    private val queue = ArrayDeque<Runnable>()

    override fun dispatch(
        context: CoroutineContext,
        block: Runnable,
    ) {
        queue.addLast(block)
    }

    fun runAll() {
        while (queue.isNotEmpty()) queue.removeFirst().run()
    }
}
