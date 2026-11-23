package net.chronoflow.feature.today.data

import kotlinx.coroutines.flow.Flow
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.model.store.TripRecord
import net.chronoflow.core.plan.trippage.PlanPreferences

interface TodaySource {
    fun synced(): Flow<Boolean>

    fun preferences(): Flow<PlanPreferences>

    fun trips(): Flow<List<TripRecord>>

    fun detail(tripId: String): Flow<TripDetail?>

    suspend fun setCheck(
        tripId: String,
        detail: TripDetail,
        blockId: String,
        status: CheckStatus?,
    )
}
