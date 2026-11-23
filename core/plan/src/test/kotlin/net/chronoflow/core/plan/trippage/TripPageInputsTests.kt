package net.chronoflow.core.plan.trippage

import org.junit.Assert.assertEquals
import org.junit.Test

class TripPageInputsTests {
    private val inputs = TripPageInputs(true, true, false, false, false, false, false, false, true, TripPageFetch.Idle)

    @Test fun noRecordDefaultsToKeepOffline() {
        assertEquals(
            TripPageState.Loading,
            inputs.copy(recordPresent = false, keepOffline = null, fetch = TripPageFetch.Offline).state(),
        )
    }

    @Test fun missingAfterSyncNeedsLoadAndSync() {
        val missing = inputs.copy(recordPresent = false, keepOffline = null, syncCompleted = true)
        assertEquals(TripPageState.NotFound, missing.state())
        assertEquals(TripPageState.Loading, missing.copy(hasLoaded = false).state())
        assertEquals(TripPageState.Loading, missing.copy(syncCompleted = false).state())
        assertEquals(TripPageState.Loading, missing.copy(awaitingSync = true).state())
    }

    @Test fun tombstoneWinsOverStoredDetail() {
        assertEquals(TripPageState.NotFound, inputs.copy(tombstoned = true, stored = true).state())
    }

    @Test fun storedNoPlan() {
        assertEquals(TripPageState.NoPlan, inputs.copy(stored = true, noPlan = true).state())
        assertEquals(TripPageState.Ready, inputs.copy(stored = true).state())
    }
}
