@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.app

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.app.perf.PerfMockSources
import net.chronoflow.core.database.testing.TestRoots
import net.chronoflow.debug.TestSeeds
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Android-only wiring regression for the shared debug/perf mock source and seed latency. */
class MockSourceLatencyTest {
    private fun latency(
        configured: Duration,
        expectedMillis: Long,
        seeding: Boolean = false,
    ) = runTest(timeout = 60.seconds) {
        val io = StandardTestDispatcher(testScheduler)
        val root = withContext(io) { TestRoots.temporaryStoreRoot() }
        try {
            val overrides = GraphOverrides(mockDelay = configured)
            val source =
                if (seeding) {
                    TestSeeds.sourceForSeed(root, overrides, io) { null }
                } else {
                    PerfMockSources(io).dataSource(root, overrides) { null }
                }
            val started = currentTime
            source.requestPasswordReset("absent@example.test")
            assertEquals(expectedMillis, currentTime - started)
        } finally {
            withContext(io) { root.deleteRecursively() }
        }
    }

    @Test fun configuredFiveSecondsIsNotClampedToEightHundredMilliseconds() = latency(5.seconds, 5_000)

    @Test fun configuredZeroDoesNotWaitThreeHundredMilliseconds() = latency(Duration.ZERO, 0)

    @Test fun seedCallsStayAtZeroDespiteConfiguredRuntimeDelay() = latency(5.seconds, 0, seeding = true)
}
