@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.core.data.repository

import app.cash.turbine.test
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.data.sync.support.SyncStore
import net.chronoflow.core.database.store.saveCheckMark
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.store.CheckMarkRecord
import net.chronoflow.core.model.store.PlanVersionRecord
import net.chronoflow.core.model.store.TripPlanSnapshot
import net.chronoflow.core.testing.sync.check
import net.chronoflow.core.testing.sync.syncTrip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class TripPlanObservationTest {
    @Test fun unknownTripEmitsAnEmptySnapshot() =
        runTest(timeout = 60.seconds) {
            val store = SyncStore(StandardTestDispatcher(testScheduler))
            try {
                TripRepository(store.database).detailValues("unknown").test {
                    assertEquals(TripPlanSnapshot(trip = null, plan = null), awaitItem())
                    cancelAndIgnoreRemainingEvents()
                }
            } finally {
                withContext(NonCancellable) { store.close() }
            }
        }

    @Test fun aNullInputHashStillObservesChecksAndPendingCheckOffs() =
        runTest(timeout = 60.seconds) {
            val store = SyncStore(StandardTestDispatcher(testScheduler))
            try {
                val repository = TripRepository(store.database)
                val trip = repository.trips().single().copy(currentInputHash = null)
                repository.upsert(trip)
                val mark = CheckMarkRecord(syncTrip, "", "sleep", "2026-11-10", 1, "done", "1")
                store.database.write { sql ->
                    sql.saveCheckMark(mark)
                    sql.saveCheckMark(mark.copy(inputHash = "old"))
                }
                val pending = check()
                store.enqueue(OutboxChange.CheckOff(pending))
                repository.detailValues(syncTrip).test {
                    val first = awaitItem()
                    assertEquals(trip, first.trip)
                    assertNull(first.plan)
                    assertEquals(listOf(mark), first.checks)
                    assertEquals(listOf(pending), first.pendingChecks)
                    val next = pending.copy(ordinal = 2)
                    store.enqueue(OutboxChange.CheckOff(next), 1)
                    runCurrent()
                    assertEquals(listOf(pending, next), expectMostRecentItem().pendingChecks)
                    cancelAndIgnoreRemainingEvents()
                }
            } finally {
                withContext(NonCancellable) { store.close() }
            }
        }

    @Test fun insertingTheCurrentPlanVersionEmitsIt() =
        runTest(timeout = 60.seconds) {
            val store = SyncStore(StandardTestDispatcher(testScheduler))
            try {
                val repository = TripRepository(store.database)
                repository.detailValues(syncTrip).test {
                    val first = awaitItem()
                    assertNotNull(first.trip)
                    assertNull(first.plan)
                    val plan = PlanVersionRecord(syncTrip, "hash", "{}".encodeToByteArray(), "now")
                    repository.store(plan, emptyList())
                    assertEquals(plan, awaitItem().plan)
                    cancelAndIgnoreRemainingEvents()
                }
            } finally {
                withContext(NonCancellable) { store.close() }
            }
        }

    @Test fun checksFromAnotherHashAreExcluded() =
        runTest(timeout = 60.seconds) {
            val store = SyncStore(StandardTestDispatcher(testScheduler))
            try {
                val repository = TripRepository(store.database)
                repository.detailValues(syncTrip).test {
                    awaitItem()
                    store.database.write { sql ->
                        sql.saveCheckMark(CheckMarkRecord(syncTrip, "old", "sleep", "2026-11-10", 1, "done", "1"))
                        sql.saveCheckMark(CheckMarkRecord(syncTrip, "hash", "sleep", "2026-11-10", 1, "done", "2"))
                    }
                    assertEquals(listOf("hash"), awaitItem().checks.map { it.inputHash })
                    cancelAndIgnoreRemainingEvents()
                }
            } finally {
                withContext(NonCancellable) { store.close() }
            }
        }

    @Test fun pendingChecksBelongToThisTripAndKeepOutboxOrder() =
        runTest(timeout = 60.seconds) {
            val store = SyncStore(StandardTestDispatcher(testScheduler))
            try {
                val repository = TripRepository(store.database)
                val trip = repository.trips().first()
                repository.upsert(trip.copy(id = "other"))
                store.enqueue(OutboxChange.CheckOff(check().copy(tripID = "other")))
                repository.detailValues(syncTrip).test {
                    assertEquals(emptyList<Any>(), awaitItem().pendingChecks)
                    val one = check()
                    val two = check().copy(ordinal = 2)
                    store.enqueue(OutboxChange.CheckOff(one), 1)
                    runCurrent()
                    assertEquals(listOf(one), expectMostRecentItem().pendingChecks)
                    store.enqueue(OutboxChange.CheckOff(two), 2)
                    runCurrent()
                    assertEquals(listOf(one, two), expectMostRecentItem().pendingChecks)
                    cancelAndIgnoreRemainingEvents()
                }
            } finally {
                withContext(NonCancellable) { store.close() }
            }
        }

    @Test fun unrelatedTripWritesDoNotEmit() =
        runTest(timeout = 60.seconds) {
            val store = SyncStore(StandardTestDispatcher(testScheduler))
            try {
                val repository = TripRepository(store.database)
                repository.detailValues(syncTrip).test {
                    val first = awaitItem()
                    val trip = first.trip!!
                    repository.upsert(trip.copy(id = "other"))
                    runCurrent()
                    expectNoEvents()
                    repository.upsert(trip.copy(revision = "changed"))
                    assertEquals("changed", awaitItem().trip?.revision)
                    cancelAndIgnoreRemainingEvents()
                }
            } finally {
                withContext(NonCancellable) { store.close() }
            }
        }

    @Test fun marksAreOrderedByMarkedAt() =
        runTest(timeout = 60.seconds) {
            val store = SyncStore(StandardTestDispatcher(testScheduler))
            try {
                val mark = CheckMarkRecord(syncTrip, "hash", "sleep", "2026-11-10", 1, "done", "2")
                store.database.write { sql ->
                    sql.saveCheckMark(mark)
                    sql.saveCheckMark(mark.copy(ordinal = 2, markedAt = "1"))
                }
                TripRepository(store.database).detailValues(syncTrip).test {
                    assertEquals(listOf("1", "2"), awaitItem().checks.map { it.markedAt })
                    cancelAndIgnoreRemainingEvents()
                }
            } finally {
                withContext(NonCancellable) { store.close() }
            }
        }

    @Test fun changingTheInputHashSelectsTheStoredPlan() =
        runTest(timeout = 60.seconds) {
            val store = SyncStore(StandardTestDispatcher(testScheduler))
            try {
                val repository = TripRepository(store.database)
                val plan = PlanVersionRecord(syncTrip, "next", "{}".encodeToByteArray(), "now")
                repository.store(plan, emptyList())
                repository.detailValues(syncTrip).test {
                    val first = awaitItem()
                    assertNull(first.plan)
                    repository.upsert(first.trip!!.copy(currentInputHash = "next"))
                    assertEquals(plan, awaitItem().plan)
                    cancelAndIgnoreRemainingEvents()
                }
            } finally {
                withContext(NonCancellable) { store.close() }
            }
        }
}
