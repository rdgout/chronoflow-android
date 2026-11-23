
package net.chronoflow.app.perf

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import net.chronoflow.core.model.CheckOffChange
import net.chronoflow.core.model.store.PlanVersionRecord
import net.chronoflow.core.model.store.TripPlanSnapshot
import net.chronoflow.core.model.store.TripRecord
import net.chronoflow.core.plan.trippage.PlanPreferences
import net.chronoflow.core.sync.PlanRetention
import net.chronoflow.core.sync.SyncResult
import net.chronoflow.core.testing.LargeTimelineFixtures
import net.chronoflow.feature.trip.page.TripPageArgs
import net.chronoflow.feature.trip.page.TripPageSource

class FixtureTripPageSource(
    private val name: String,
    private val bytes: ByteArray? = null,
) : TripPageSource {
    private val retention = PlanRetention()
    private val pending = MutableStateFlow<List<CheckOffChange>>(emptyList())

    fun enqueueCheck(change: CheckOffChange) {
        pending.update { it + change }
    }

    override fun snapshots(tripId: String) =
        flow {
            val id = "fixture-$name"
            val snapshot =
                TripPlanSnapshot(
                    trip =
                        TripRecord(
                            id,
                            "{}",
                            currentInputHash = id,
                            keepOffline = true,
                            departureUtc = "2026-01-01T00:00:00Z",
                        ),
                    plan =
                        PlanVersionRecord(
                            id,
                            id,
                            bytes ?: LargeTimelineFixtures.detailBytes(name),
                            "2026-01-01T00:00:00Z",
                        ),
                )
            emitAll(pending.map { snapshot.copy(pendingChecks = it) })
        }.flowOn(Dispatchers.Default)

    override fun preferences() = flowOf(PlanPreferences())

    override fun hold(tripId: String) = retention.hold(tripId)

    override suspend fun refresh(): SyncResult? = null
}

internal data class DiagnosticTripPageInput(
    val bytes: ByteArray,
    val args: TripPageArgs,
) {
    companion object {
        fun load(name: String): DiagnosticTripPageInput =
            DiagnosticTripPageInput(
                LargeTimelineFixtures.detailBytes(name),
                TripPageArgs(
                    "fixture-$name",
                    dayId =
                        LargeTimelineFixtures
                            .fixture(name)
                            .detail.plan.days
                            .firstOrNull()
                            ?.id,
                ),
            )
    }
}
