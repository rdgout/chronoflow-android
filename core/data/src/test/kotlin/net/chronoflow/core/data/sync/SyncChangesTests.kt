package net.chronoflow.core.data.sync

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.data.sync.support.EngineFixture
import net.chronoflow.core.data.sync.support.SyncStore
import net.chronoflow.core.data.sync.support.syncMockSource
import net.chronoflow.core.datamock.MockDataSource
import net.chronoflow.core.sync.FullPull
import net.chronoflow.core.sync.SyncChanges
import net.chronoflow.core.sync.SyncEngine
import net.chronoflow.core.sync.SyncScheduler
import net.chronoflow.core.testing.sync.ManualSyncScheduler
import net.chronoflow.core.testing.sync.card
import net.chronoflow.core.testing.sync.detail
import net.chronoflow.core.testing.sync.syncDate
import net.chronoflow.core.testing.sync.syncTrip
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SyncChangesTests {
    private fun mockTest(
        remoteTripUpdate: suspend () -> Boolean = { false },
        block: suspend TestScope.(SyncStore, MockDataSource, SyncEngine) -> Unit,
    ) = runTest(timeout = 60.seconds) {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val store = SyncStore(dispatcher)
        val source = syncMockSource(dispatcher, remoteTripUpdate = remoteTripUpdate)
        val scheduler = ManualSyncScheduler()
        val engine =
            SyncEngine(
                store.sync,
                source,
                store.epoch,
                scheduler = SyncScheduler({ syncDate }, scheduler::sleep),
                dispatcher = dispatcher,
            )
        try {
            block(store, source, engine)
        } finally {
            withContext(NonCancellable) {
                engine.cancelAndWait()
                store.close()
            }
        }
    }

    @Test fun unchangedPullIgnoresReceiptTimes() =
        mockTest { store, source, engine ->
            FullPull(store.sync, source, now = { syncDate }).run(store.epoch)
            val result = FullPull(store.sync, source, now = { syncDate.plusSeconds(60) }).run(store.epoch)
            assertEquals(SyncChanges(), result.changes)
            assertEquals(SyncChanges(), engine.refreshNow().changes)
        }

    @Test fun mockRefreshSwitchChangesServerTripAndRevisionOnce() {
        var armed = false
        mockTest(remoteTripUpdate = { armed.also { armed = false } }) { _, source, engine ->
            engine.refreshNow()
            assertEquals(SyncChanges(), engine.refreshNow().changes)
            armed = true
            assertEquals(SyncChanges(tripsUpdated = 1, planVersionsChanged = 1), engine.refreshNow().changes)
            assertEquals(
                "CF123",
                source
                    .fetchTrips()
                    .first()
                    .card.flightNumber,
            )
            assertEquals("refresh-CF123", source.fetchTripDetail(syncTrip).detail.detailVersion)
            assertEquals(SyncChanges(), engine.refreshNow().changes)
        }
    }

    @Test fun oneTripUpdatedAndThenRemovedOnMockServer() =
        mockTest { _, source, engine ->
            engine.refreshNow()
            val updated = card().let { it.copy(card = it.card.copy(flightNumber = "CF123")) }
            source.replaceRemoteTrips(listOf(updated), mapOf(syncTrip to detail()))
            assertEquals(SyncChanges(tripsUpdated = 1), engine.refreshNow().changes)
            source.replaceRemoteTrips(emptyList(), emptyMap())
            assertEquals(SyncChanges(tripsRemoved = 1), engine.refreshNow().changes)
            assertEquals(SyncChanges(), engine.refreshNow().changes)
            source.replaceRemoteTrips(listOf(updated), mapOf(syncTrip to detail()))
            assertEquals(SyncChanges(tripsAdded = 1), engine.refreshNow().changes)
        }

    @Test fun planHashOrDetailVersionCountsAsOneUpdatedTripFalse() =
        planHashOrDetailVersionCountsAsOneUpdatedTrip(
            false,
        )

    @Test fun planHashOrDetailVersionCountsAsOneUpdatedTripTrue() = planHashOrDetailVersionCountsAsOneUpdatedTrip(true)

    private fun planHashOrDetailVersionCountsAsOneUpdatedTrip(changeHash: Boolean) =
        mockTest { _, source, engine ->
            engine.refreshNow()
            val hash = if (changeHash) "new-hash" else "hash"
            source.replaceRemoteTrips(listOf(card(hash = hash, version = "v2")), mapOf(syncTrip to detail(hash, "v2")))
            assertEquals(SyncChanges(tripsUpdated = 1, planVersionsChanged = 1), engine.refreshNow().changes)
            assertEquals(SyncChanges(), engine.refreshNow().changes)
        }

    @Test fun downloadingOrEvictingAnUnchangedPlanIsNotAnUpdate() =
        mockTest { store, source, _ ->
            val pull = FullPull(store.sync, source, now = { syncDate })
            pull.run(store.epoch)
            source.replaceRemoteTrips(listOf(card(keep = false)), mapOf(syncTrip to detail()))
            assertEquals(SyncChanges(), pull.run(store.epoch).changes)
            source.replaceRemoteTrips(listOf(card()), mapOf(syncTrip to detail()))
            assertEquals(SyncChanges(), pull.run(store.epoch).changes)
        }

    @Test fun joinedRefreshCountsAChangedTripOnceAcrossReruns() =
        runTest(timeout = 60.seconds) {
            val f = EngineFixture.make(this)
            try {
                f.engine.refreshNow()
                val gate = f.gate()
                f.source.configure(
                    trips = listOf(card(version = "v2")),
                    detail = detail(version = "v2"),
                    hook = { if (it == "identity") gate.block() },
                )
                val first = async { f.engine.refreshNow() }
                gate.waitUntilBlocked()
                val second = async { f.engine.refreshNow() }
                runCurrent()
                f.source.configure()
                gate.release()
                assertEquals(SyncChanges(tripsUpdated = 1, planVersionsChanged = 1), first.await().changes)
                assertEquals(SyncChanges(tripsUpdated = 1, planVersionsChanged = 1), second.await().changes)
            } finally {
                f.close()
            }
        }
}
