@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.trip.state

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.device.DeviceIdentityFailure
import net.chronoflow.core.model.session.SessionFailure
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.core.plan.trippage.TripPageFetch
import net.chronoflow.core.plan.trippage.TripPageInputs
import net.chronoflow.core.plan.trippage.TripPageState
import net.chronoflow.core.sync.SyncPathMonitoring
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.security.GeneralSecurityException
import kotlin.time.Duration.Companion.seconds

class FakePath : SyncPathMonitoring {
    val values = Channel<Boolean>(Channel.UNLIMITED)
    var stopped = false

    override fun updates() = values.receiveAsFlow()

    override suspend fun stop() {
        stopped = true
        values.close()
    }
}

class TripPageFetchTests {
    private class DetailRequests {
        var count = 0

        suspend fun fetch(
            @Suppress("UNUSED_PARAMETER") id: String,
        ) {
            count++
            if (count == 1) throw DataFailure.Offline
        }
    }

    private fun state(
        model: TripPageFetchModel,
        present: Boolean = true,
        awaiting: Boolean = false,
    ) = TripPageInputs(
        true,
        present,
        if (present) false else null,
        false,
        false,
        false,
        true,
        awaiting,
        model.online.value,
        model.fetch.value,
    ).state()

    @Test fun injectedOfflinePathAndClockStayFrozenAfterStarting() =
        runTest(timeout = 60.seconds) {
            val requests = DetailRequests()
            val model = TripPageFetchModel("trip", requests::fetch, null, backgroundScope)
            model.setOnline(false)
            model.start()
            runCurrent()
            model.start()
            runCurrent()
            assertFalse(model.online.value)
            assertEquals(TripPageState.RemoteOffline, state(model))
            assertEquals(0, requests.count)
            model.setOnline(true)
            runCurrent()
            assertEquals(TripPageFetch.Offline, model.fetch.value)
            assertEquals(TripPageState.RemoteOffline, state(model))
            assertEquals(1, requests.count)
            model.retry()
            runCurrent()
            assertEquals(TripPageFetch.Loaded, model.fetch.value)
            assertEquals(TripPageState.RemoteLoading, state(model))
            assertEquals(2, requests.count)
            model.stop()
        }

    @Test fun offlineFailureReconnectAndLivePreferences() =
        runTest(timeout = 60.seconds) {
            val requests = DetailRequests()
            val model = TripPageFetchModel("trip", requests::fetch, null, backgroundScope)
            model.start()
            runCurrent()
            assertTrue(model.online.value)
            assertEquals(TripPageFetch.Offline, model.fetch.value)
            assertEquals(TripPageState.Loading, state(model, present = false, awaiting = true))
            assertEquals(TripPageState.RemoteOffline, state(model))
            model.setOnline(false)
            model.setOnline(true)
            runCurrent()
            assertEquals(TripPageFetch.Loaded, model.fetch.value)
            assertEquals(2, requests.count)
            model.setOnline(true)
            runCurrent()
            assertEquals(2, requests.count)
            model.stop()
        }

    @Test fun awaitingSyncOnlySuppressesMissing() {
        fun resolve(fetch: TripPageFetch) =
            TripPageState.resolve(
                false,
                false,
                online = true,
                fetch = fetch,
                missingAfterSync = true,
                awaitingSync = true,
            )
        assertEquals(TripPageState.RemoteOffline, resolve(TripPageFetch.Offline))
        assertEquals(TripPageState.Server, resolve(TripPageFetch.Failed))
        assertEquals(TripPageState.NotFound, resolve(TripPageFetch.NotFound))
        assertEquals(TripPageState.RemoteLoading, resolve(TripPageFetch.Loading))
    }

    @Test fun notFoundAndServerMapping() =
        runTest(timeout = 60.seconds) {
            suspend fun mapped(error: Exception): TripPageFetch {
                val model = TripPageFetchModel("trip", { throw error }, null, backgroundScope)
                model.start()
                runCurrent()
                return model.fetch.value.also { model.stop() }
            }
            assertEquals(TripPageFetch.NotFound, mapped(DataFailure.Server(404)))
            assertEquals(TripPageFetch.Failed, mapped(DataFailure.Server(500)))
            assertEquals(TripPageFetch.Failed, mapped(DataFailure.Server(null)))
            assertEquals(TripPageFetch.Failed, mapped(IOException()))
            assertEquals(TripPageFetch.Failed, mapped(StoreFailure.Closed))
            assertEquals(TripPageFetch.Failed, mapped(SessionFailure.DatabaseUnavailable))
            assertEquals(TripPageFetch.Failed, mapped(DeviceIdentityFailure.InvalidRandomness))
            assertEquals(TripPageFetch.Failed, mapped(GeneralSecurityException()))
            assertEquals(TripPageFetch.Failed, mapped(IllegalStateException()))
        }

    @Test fun reconnectDuringFetchRetriesOnceAfterIt() =
        runTest(timeout = 60.seconds) {
            val gate = CompletableDeferred<Unit>()
            var requests = 0
            val model =
                TripPageFetchModel("trip", {
                    requests++
                    if (requests == 1) {
                        gate.await()
                        throw DataFailure.Offline
                    }
                }, null, backgroundScope)
            model.start()
            runCurrent()
            model.setOnline(false)
            model.setOnline(true)
            assertEquals(1, requests)
            gate.complete(Unit)
            runCurrent()
            assertEquals(2, requests)
            assertEquals(TripPageFetch.Loaded, model.fetch.value)
            model.stop()
        }

    @Test fun stopCancelsFetchAndPath() =
        runTest(timeout = 60.seconds) {
            val path = FakePath()
            var cancelled = false
            var requests = 0
            val model =
                TripPageFetchModel("trip", {
                    requests++
                    try {
                        CompletableDeferred<Unit>().await()
                    } finally {
                        cancelled = true
                    }
                }, path, backgroundScope)
            model.start()
            runCurrent()
            model.setOnline(false)
            model.stop()
            runCurrent()
            assertTrue(cancelled)
            assertTrue(path.stopped)
            assertEquals(TripPageFetch.Loading, model.fetch.value)
            model.setOnline(true)
            runCurrent()
            assertEquals(1, requests)
        }

    @Test fun cancellationIsRethrown() =
        runTest(timeout = 60.seconds) {
            val model = TripPageFetchModel("trip", { throw CancellationException() }, null, backgroundScope)
            model.start()
            runCurrent()
            assertEquals(TripPageFetch.Loading, model.fetch.value)
            model.stop()
        }
}
