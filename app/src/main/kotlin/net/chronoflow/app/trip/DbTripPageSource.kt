package net.chronoflow.app.trip

import net.chronoflow.app.SessionGraph
import net.chronoflow.feature.trip.page.TripPageSource

class DbTripPageSource(
    private val session: SessionGraph,
) : TripPageSource {
    override fun snapshots(tripId: String) = session.trips.detailValues(tripId)

    override fun preferences() = session.settings.planPreferences()

    override fun hold(tripId: String) = session.sync.retention.hold(tripId)

    override suspend fun refresh() = session.sync.refreshNow()
}
