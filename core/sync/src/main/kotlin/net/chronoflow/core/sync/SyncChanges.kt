package net.chronoflow.core.sync

import net.chronoflow.core.model.store.TripRecord

data class SyncChanges(
    val tripsAdded: Int = 0,
    val tripsUpdated: Int = 0,
    val tripsRemoved: Int = 0,
    val planVersionsChanged: Int = 0,
)

class SyncSnapshot(
    private val trips: Map<String, Value>,
) {
    data class Value(
        val trip: TripRecord,
        val planVersion: String?,
    )

    fun changes(to: SyncSnapshot): SyncChanges {
        val beforeIDs = trips.keys
        val afterIDs = to.trips.keys
        val common = beforeIDs.intersect(afterIDs)
        val plans =
            common
                .filter { id ->
                    val before = trips.getValue(id)
                    val after = to.trips.getValue(id)
                    before.trip.currentInputHash != after.trip.currentInputHash ||
                        (
                            before.planVersion != null && after.planVersion != null &&
                                before.planVersion != after.planVersion
                        )
                }.toSet()
        return SyncChanges(
            tripsAdded = (afterIDs - beforeIDs).size,
            tripsUpdated = common.count { trips.getValue(it).trip != to.trips.getValue(it).trip || it in plans },
            tripsRemoved = (beforeIDs - afterIDs).size,
            planVersionsChanged = plans.size,
        )
    }
}
