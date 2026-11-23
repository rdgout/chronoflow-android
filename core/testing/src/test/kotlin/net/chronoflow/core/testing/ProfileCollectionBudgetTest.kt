package net.chronoflow.core.testing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.concurrent.TimeUnit

class ProfileCollectionBudgetTest {
    @Test fun lastPermittedIterationFitsAfterEarlierIterationsUseTheirFullBudgets() {
        val budget = ProfileCollectionBudget(maxIterations = 15, stableIterations = 3)
        var now = 0L
        BoundedProfileInput(
            stepTimeoutMillis = budget.collectorTimeoutMillis,
            journeyTimeoutMillis = budget.collectorTimeoutMillis,
            nowNanos = { now },
        ).use { collector ->
            now =
                TimeUnit.MILLISECONDS.toNanos(
                    budget.setupTimeoutMillis +
                        (budget.maxIterations - 1) * (budget.journeyTimeoutMillis + budget.iterationOverheadMillis),
                )
            assertEquals(
                42,
                collector.step("last journey") {
                    now += TimeUnit.MILLISECONDS.toNanos(budget.journeyTimeoutMillis)
                    42
                },
            )
            collector.step("last ART flush") { now += TimeUnit.MILLISECONDS.toNanos(budget.iterationOverheadMillis) }
            assertThrows(AssertionError::class.java) { collector.step("beyond the collection budget") {} }
        }
    }

    @Test fun impossibleStabilityPolicyFailsBeforeCollection() {
        assertThrows(IllegalArgumentException::class.java) {
            ProfileCollectionBudget(maxIterations = 2, stableIterations = 3)
        }
    }
}
