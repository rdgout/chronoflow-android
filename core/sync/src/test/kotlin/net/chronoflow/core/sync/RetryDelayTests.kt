package net.chronoflow.core.sync

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class RetryDelayTests(
    private val attempts: Int,
    private val server: Double,
    private val expected: Double,
) {
    @Test fun retryDelay() {
        assertEquals(expected, OutboxFailurePolicy.retryDelay(attempts, server), 0.0)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "attempts={0}, server={1}")
        fun rows(): List<Array<Any>> =
            listOf(
                arrayOf(0, 0.0, 1.0),
                arrayOf(1, 0.0, 2.0),
                arrayOf(2, 0.0, 4.0),
                arrayOf(3, 0.0, 8.0),
                arrayOf(4, 0.0, 16.0),
                arrayOf(5, 0.0, 32.0),
                arrayOf(6, 0.0, 64.0),
                arrayOf(7, 0.0, 128.0),
                arrayOf(8, 0.0, 256.0),
                arrayOf(9, 0.0, 300.0),
                arrayOf(10, 0.0, 300.0),
                arrayOf(0, 900.0, 900.0),
                arrayOf(-1, 0.0, 1.0),
            )
    }
}
