package net.chronoflow.core.network.support

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.serialization.json.JsonObject
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.network.ApiBaseUrl
import net.chronoflow.core.network.ApiClient
import net.chronoflow.core.network.ApiDeadline
import net.chronoflow.core.testing.Fixtures
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.OkHttpClient
import org.junit.rules.ExternalResource
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration

class MockApi : ExternalResource() {
    val server = MockWebServer()
    private val routes = ConcurrentHashMap<Pair<String, String>, (RecordedRequest) -> MockResponse>()
    val requests = CopyOnWriteArrayList<RecordedRequest>()
    private val unmatched = AtomicInteger()
    private val routeFailures = CopyOnWriteArrayList<Throwable>()
    val baseUrl: ApiBaseUrl get() = ApiBaseUrl.parse(server.url("/v1").toString(), false)

    override fun before() {
        server.dispatcher =
            object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    requests += request
                    val route = routes[request.method to request.url.encodedPath]
                    if (route == null) {
                        unmatched.incrementAndGet()
                        return response(599)
                    }
                    return try {
                        route(request)
                    } catch (e: AssertionError) {
                        routeFailures += e
                        response(599)
                    } catch (e: IllegalArgumentException) {
                        routeFailures += e
                        response(599)
                    }
                }
            }
        server.start()
    }

    override fun after() {
        try {
            routeFailures.firstOrNull()?.let { throw AssertionError("route assertion failed", it) }
            check(unmatched.get() == 0) { "unmatched HTTP request" }
        } finally {
            server.close()
        }
    }

    fun route(
        method: String,
        path: String,
        reply: (RecordedRequest) -> MockResponse,
    ) {
        routes[method to path] = reply
    }

    fun reply(
        method: String,
        path: String,
        status: Int = 200,
        body: String = "",
    ) {
        route(method, path) { response(status, body) }
    }

    fun client(
        bearer: suspend () -> String? = { "artificial-old-token" },
        deadline: ApiDeadline = ApiDeadline { awaitCancellation() },
        base: () -> OkHttpClient = { OkHttpClient() },
        now: () -> Instant = { Instant.EPOCH },
    ) = ApiClient(baseUrl, base, bearer, Dispatchers.IO, deadline, now)
}

fun response(
    status: Int = 200,
    body: String = "",
): MockResponse =
    MockResponse
        .Builder()
        .code(status)
        .body(body)
        .build()

fun json(name: String): String = Fixtures.text(name)

fun requestJson(recorded: RecordedRequest): JsonObject? =
    recorded.body
        ?.utf8()
        ?.takeIf { it.isNotEmpty() }
        ?.let { ApiJson.response.parseToJsonElement(it) as JsonObject }

class RecordingDeadline : ApiDeadline {
    val durations = CopyOnWriteArrayList<Duration>()

    override suspend fun await(timeout: Duration) {
        durations += timeout
        awaitCancellation()
    }
}

class EventRecorder : EventListener() {
    val canceled = AtomicInteger()

    override fun canceled(call: Call) {
        canceled.incrementAndGet()
    }
}

suspend fun failure(block: suspend () -> Any?): DataFailure =
    try {
        block()
        error("expected DataFailure")
    } catch (failure: DataFailure) {
        failure
    }
