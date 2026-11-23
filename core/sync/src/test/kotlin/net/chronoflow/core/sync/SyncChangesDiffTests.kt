package net.chronoflow.core.sync

import net.chronoflow.core.model.store.TripRecord
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncChangesDiffTests {
    private val trip = TripRecord("trip", "{}", currentInputHash = "hash", departureUtc = "now")

    private fun snapshot(
        record: TripRecord = trip,
        version: String? = "v1",
    ) = SyncSnapshot(
        mapOf(record.id to SyncSnapshot.Value(record, version)),
    )

    @Test fun added() {
        assertEquals(SyncChanges(tripsAdded = 1), SyncSnapshot(emptyMap()).changes(snapshot()))
    }

    @Test fun removed() {
        assertEquals(SyncChanges(tripsRemoved = 1), snapshot().changes(SyncSnapshot(emptyMap())))
    }

    @Test fun updated() {
        assertEquals(SyncChanges(tripsUpdated = 1), snapshot().changes(snapshot(trip.copy(card = "null"))))
    }

    @Test fun unchanged() {
        assertEquals(SyncChanges(), snapshot().changes(snapshot()))
    }

    @Test fun inputHashChanged() {
        assertEquals(
            SyncChanges(tripsUpdated = 1, planVersionsChanged = 1),
            snapshot().changes(snapshot(trip.copy(currentInputHash = "other"))),
        )
    }

    @Test fun detailVersionChanged() {
        assertEquals(
            SyncChanges(tripsUpdated = 1, planVersionsChanged = 1),
            snapshot().changes(snapshot(version = "v2")),
        )
    }

    @Test fun downloadedVersionIsNotAChange() {
        assertEquals(SyncChanges(), snapshot(version = null).changes(snapshot()))
    }
}
