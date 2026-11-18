package net.chronoflow.core.datamock

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/** Android seeded-latency helper coverage (new). */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MockLatencyTest {
    @Test fun zeroIgnoresClamp() =
        runTest(timeout = 60.seconds) {
            MockLatency.exact(0.0)(0.5)
            assertEquals(0L, testScheduler.currentTime)
        }

    @Test fun exactUsesVirtualTime() =
        runTest(timeout = 60.seconds) {
            MockLatency.exact(2.5)(0.3)
            assertEquals(2500L, testScheduler.currentTime)
        }
}
