package net.chronoflow.core.data.sync

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import net.chronoflow.core.data.repository.AccountRepository
import net.chronoflow.core.data.repository.OutboxRepository
import net.chronoflow.core.data.repository.SettingsRepository
import net.chronoflow.core.data.repository.TripRepository
import net.chronoflow.core.data.sync.support.SyncStore
import net.chronoflow.core.data.sync.support.syncMockSource
import net.chronoflow.core.datamock.MemoryMockStateStore
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.model.api.AccountIdentity
import net.chronoflow.core.model.api.AccountPolicies
import net.chronoflow.core.model.api.AccountSnapshot
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.model.contracts.TripListResponse
import net.chronoflow.core.model.store.OutboxPayload
import net.chronoflow.core.network.ApiBaseUrl
import net.chronoflow.core.network.ApiClient
import net.chronoflow.core.network.ApiDataSource
import net.chronoflow.core.network.ApiDeadline
import net.chronoflow.core.sync.SyncEngine
import net.chronoflow.core.sync.SyncScheduler
import net.chronoflow.core.sync.SyncState
import net.chronoflow.core.testing.sync.ManualSyncScheduler
import net.chronoflow.core.testing.sync.card
import net.chronoflow.core.testing.sync.check
import net.chronoflow.core.testing.sync.detail
import net.chronoflow.core.testing.sync.identity
import net.chronoflow.core.testing.sync.snapshot
import net.chronoflow.core.testing.sync.syncAccount
import net.chronoflow.core.testing.sync.syncDate
import net.chronoflow.core.testing.sync.syncTrip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

class DataSourceIntegrationTests {
    @Test fun mockServerAbsoluteReplayAndOfflineRestart() =
        runTest(timeout = 60.seconds) {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val store = SyncStore(dispatcher)
            val source = syncMockSource(dispatcher, MemoryMockStateStore())
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
                assertEquals(SyncState.Idle, engine.refreshNow().state)
                val row = store.enqueue(OutboxChange.CheckOff(check()))
                val accepted = source.send(OutboxPayload.change(row))
                assertEquals(accepted, source.send(OutboxPayload.change(row)))
                assertEquals(0, engine.refreshNow().pendingCount)
                assertEquals(1, TripRepository(store.database).checks(syncTrip, "hash").size)
                source.setOffline(true)
                store.enqueue(
                    OutboxChange.Settings(
                        SettingsSection.Preferences,
                        JsonObject(mapOf("time_format" to JsonPrimitive("12"))),
                    ),
                    1,
                )
                assertEquals(SyncState.Offline, engine.refreshNow().state)
                engine.cancelAndWait()
                store.database.close()
                store.database.open(syncAccount)
                val restored = SettingsRepository(store.database).settings().first { it.section == "preferences" }
                val value = ApiJson.response.parseToJsonElement(restored.value) as JsonObject
                assertEquals(JsonPrimitive("12"), value["timeFormat"])
                assertEquals(1, OutboxRepository(store.database).pending().size)
            } finally {
                withContext(NonCancellable) {
                    engine.cancelAndWait()
                    store.close()
                }
            }
        }

    @Test fun apiSourcePullAndOutboxThroughDenyByDefaultProtocol() =
        runTest(timeout = 60.seconds) {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val store = SyncStore(dispatcher)
            val server = MockWebServer()
            val requests = CopyOnWriteArrayList<RecordedRequest>()
            val unmatched = AtomicInteger()
            val tripList = TripListResponse(listOf(card()))
            val listBody = ApiJson.response.encodeToString(TripListResponse.serializer(), tripList)
            val identityBody = ApiJson.response.encodeToString(AccountIdentity.serializer(), identity())
            val accountBody = ApiJson.response.encodeToString(AccountSnapshot.serializer(), snapshot())
            val detailBody = ApiJson.response.encodeToString(TripDetail.serializer(), detail())
            val replies =
                mapOf(
                    ("GET" to "/v1/users/me") to identityBody,
                    ("GET" to "/v1/account") to accountBody,
                    ("GET" to "/v1/trips") to listBody,
                    ("GET" to "/v1/trips/$syncTrip") to detailBody,
                )
            server.dispatcher =
                object : Dispatcher() {
                    override fun dispatch(request: RecordedRequest): MockResponse {
                        requests += request
                        val route = request.method to request.url.encodedPath
                        if (route == ("POST" to "/v1/walkthrough")) return MockResponse.Builder().code(204).build()
                        val body = replies[route]
                        if (body == null) {
                            unmatched.incrementAndGet()
                            return MockResponse.Builder().code(599).build()
                        }
                        return MockResponse
                            .Builder()
                            .code(200)
                            .body(body)
                            .build()
                    }
                }
            server.start()
            val client =
                ApiClient(
                    ApiBaseUrl.parse(server.url("/v1").toString(), release = false),
                    bearer = { "fixture-bearer" },
                    io = dispatcher,
                    deadline = ApiDeadline { awaitCancellation() },
                    now = { syncDate },
                )
            val scheduler = ManualSyncScheduler()
            val engine =
                SyncEngine(
                    store.sync,
                    ApiDataSource(client),
                    store.epoch,
                    scheduler = SyncScheduler({ syncDate }, scheduler::sleep),
                    dispatcher = dispatcher,
                )
            try {
                store.enqueue(OutboxChange.Walkthrough)
                assertEquals(SyncState.Idle, engine.refreshNow().state)
                assertEquals(
                    listOf(
                        "POST /v1/walkthrough",
                        "GET /v1/users/me",
                        "GET /v1/account",
                        "GET /v1/trips",
                        "GET /v1/trips/$syncTrip",
                    ),
                    requests.map { it.method + " " + it.url.encodedPath },
                )
                assertTrue(requests.all { it.headers["Authorization"] == "Bearer fixture-bearer" })
                assertEquals(0, unmatched.get())
                assertNotNull(AccountRepository(store.database).account()?.syncedAt)
                assertTrue(OutboxRepository(store.database).pending().isEmpty())
            } finally {
                withContext(NonCancellable) {
                    engine.cancelAndWait()
                    client.evictIdleConnections()
                    server.close()
                    store.close()
                }
            }
        }
}
