package net.chronoflow.feature.trip.page

import kotlinx.coroutines.flow.MutableStateFlow
import net.chronoflow.core.model.store.PlanVersionRecord
import net.chronoflow.core.model.store.TripPlanSnapshot
import net.chronoflow.core.model.store.TripRecord
import net.chronoflow.core.plan.trippage.PlanPreferences
import net.chronoflow.core.sync.PlanRetention
import net.chronoflow.core.testing.LargeTimelineFixtures

class FakeTripPageSource(
    initial: TripPlanSnapshot,
) : TripPageSource {
    val snapshot = MutableStateFlow(initial)
    val settings = MutableStateFlow(PlanPreferences())
    val retention = PlanRetention()
    var holds = 0
        private set
    var refreshes = 0
        private set
    val releases: Int get() = holds - retention.withHeldPlans { it.size }

    override fun snapshots(tripId: String) = snapshot

    override fun preferences() = settings

    override fun hold(tripId: String) = retention.hold(tripId).also { holds++ }

    override suspend fun refresh() {
        refreshes++
    }

    companion object {
        fun fixture(name: String): FakeTripPageSource {
            val id = "fixture-$name"
            return FakeTripPageSource(
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
                            LargeTimelineFixtures.detailBytes(name),
                            "2026-01-01T00:00:00Z",
                        ),
                ),
            )
        }
    }
}
