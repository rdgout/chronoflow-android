package net.chronoflow.feature.trips

import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.model.store.TripPlanSnapshot
import net.chronoflow.core.model.store.TripRecord
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.plan.PlanClock
import net.chronoflow.core.plan.PlanDecoder
import net.chronoflow.core.sync.SyncResult
import net.chronoflow.core.sync.refresh.RefreshDebugProbe

/** The app supplies the session-bound implementation through the same CreationExtras convention as auth. */
interface TripsDependencies {
    fun tripRecords(): Flow<List<TripRecord>>

    fun planSnapshots(tripId: String): Flow<TripPlanSnapshot>

    fun synced(): Flow<Boolean>

    fun preferences(): Flow<TripsPreferences>

    val clock: PlanClock
    val decoder: PlanDecoder

    suspend fun formatter(): CopyLineFormatter

    suspend fun refreshNow(): SyncResult?

    val refreshProbe: RefreshDebugProbe
    val worker: CoroutineDispatcher
}

object TripsDependenciesKey : CreationExtras.Key<TripsDependencies>

data class TripsPreferences(
    val hidden: List<BlockType> = emptyList(),
    val timeFormat: TimeFormat = TimeFormat.Value24,
)
