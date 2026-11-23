package net.chronoflow.core.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class PlanLeaseTests {
    @Test fun referenceCountsAndIdempotentRelease() {
        val retention = PlanRetention()
        val first = retention.hold("trip")
        val second = retention.hold("trip")
        assertEquals(setOf("trip"), retention.withHeldPlans { it })
        first.release()
        first.release()
        assertEquals(setOf("trip"), retention.withHeldPlans { it })
        second.release()
        assertEquals(emptySet<String>(), retention.withHeldPlans { it })
    }

    @Test fun clearDropsEveryHold() {
        val retention = PlanRetention()
        val lease = retention.hold("trip")
        retention.clear()
        assertEquals(emptySet<String>(), retention.withHeldPlans { it })
        lease.release()
    }
}
