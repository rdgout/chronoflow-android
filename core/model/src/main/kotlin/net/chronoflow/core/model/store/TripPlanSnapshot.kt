package net.chronoflow.core.model.store

import net.chronoflow.core.model.CheckOffChange

data class TripPlanSnapshot(
    val plan: PlanVersionRecord?,
    val checks: List<CheckMarkRecord> = emptyList(),
    val pendingChecks: List<CheckOffChange> = emptyList(),
)
