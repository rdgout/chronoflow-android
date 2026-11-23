package net.chronoflow.core.model.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

internal fun assertInvalidationWaits(
    admit: (() -> Unit) -> Unit,
    invalidate: () -> Unit,
) {
    val entered = CountDownLatch(1)
    val release = CountDownLatch(1)
    val executor = Executors.newSingleThreadExecutor()
    var invalidator: Thread? = null
    try {
        val admitted =
            executor.submit {
                admit {
                    entered.countDown()
                    check(release.await(1, TimeUnit.SECONDS))
                }
            }
        assertTrue(entered.await(1, TimeUnit.SECONDS))
        invalidator = Thread(invalidate).apply { start() }
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1)
        while (invalidator.state != Thread.State.WAITING && invalidator.isAlive &&
            System.nanoTime() < deadline
        ) {
            Thread.onSpinWait()
        }
        assertEquals(Thread.State.WAITING, invalidator.state)
        assertTrue(invalidator.isAlive)
        release.countDown()
        admitted.get(1, TimeUnit.SECONDS)
        invalidator.join(1000)
        assertFalse(invalidator.isAlive)
    } finally {
        release.countDown()
        invalidator?.join(1000)
        executor.shutdownNow()
    }
}
