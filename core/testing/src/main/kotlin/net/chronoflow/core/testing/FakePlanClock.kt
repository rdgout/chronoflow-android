package net.chronoflow.core.testing

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import net.chronoflow.core.plan.PlanClock
import java.time.Instant

class FakePlanClock(
    start: Instant,
) : PlanClock {
    private val shared = MutableSharedFlow<Instant>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    @Volatile
    var now: Instant = start
        private set

    init {
        shared.tryEmit(start)
    }

    override fun values(): Flow<Instant> = shared.asSharedFlow()

    fun advance(to: Instant) {
        now = to
        shared.tryEmit(to)
    }
}
