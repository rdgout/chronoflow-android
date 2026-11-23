package net.chronoflow.core.plan

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.Instant

class FixedPlanClock(
    val now: Instant,
) : PlanClock {
    override fun values(): Flow<Instant> = flowOf(now)
}
