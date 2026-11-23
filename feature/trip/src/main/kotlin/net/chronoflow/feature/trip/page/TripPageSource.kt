package net.chronoflow.feature.trip.page

import kotlinx.coroutines.flow.Flow
import net.chronoflow.core.model.store.TripPlanSnapshot
import net.chronoflow.core.plan.trippage.PlanPreferences
import net.chronoflow.core.sync.PlanLease
import net.chronoflow.core.sync.SyncResult

interface TripPageSource {
    fun snapshots(tripId: String): Flow<TripPlanSnapshot>

    fun preferences(): Flow<PlanPreferences>

    fun hold(tripId: String): PlanLease

    suspend fun refresh(): SyncResult?
}
