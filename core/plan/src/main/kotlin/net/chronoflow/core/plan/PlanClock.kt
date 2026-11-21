package net.chronoflow.core.plan

import kotlinx.coroutines.flow.Flow
import java.time.Instant

// Emit the current instant on collection, then again whenever the plan time should be re-read.
interface PlanClock {
    val now: Instant

    fun values(): Flow<Instant>
}
