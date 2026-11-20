@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.core.wizard.model

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestCoroutineScheduler

class CheckingTestClock(
    private val scheduler: TestCoroutineScheduler,
) {
    private val waiters = mutableListOf<Long>()
    val now: Long get() = scheduler.currentTime
    val waiterCount: Int get() = waiters.size
    val deadlines: List<Long> get() = waiters.toList()
    val clock =
        WizardCheckingClock({ scheduler.currentTime }) { deadline ->
            waiters.add(deadline)
            try {
                delay((deadline - scheduler.currentTime).coerceAtLeast(0))
            } finally {
                waiters.remove(deadline)
            }
        }

    fun advance(toMillis: Long) {
        require(toMillis >= scheduler.currentTime)
        scheduler.advanceTimeBy(toMillis - scheduler.currentTime)
        scheduler.runCurrent()
    }
}
