@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.core.network

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.SocketEffect
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.api.SessionAccount
import net.chronoflow.core.network.support.EventRecorder
import net.chronoflow.core.network.support.MockApi
import net.chronoflow.core.network.support.RecordingDeadline
import net.chronoflow.core.network.support.failure
import net.chronoflow.core.network.support.json
import net.chronoflow.core.network.support.requestJson
import net.chronoflow.core.network.support.response
import okhttp3.Cache
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class ApiClientTests {
    @get:Rule val api = MockApi()

    @get:Rule val folder = TemporaryFolder()

    @Test fun dedicatedPoolUsesTheDefaultKeepAliveBelowTheCloudflareEdgeLimitWithoutRetries() =
        runTest(timeout = 60.seconds) {
            val base =
                withContext(Dispatchers.IO) {
                    OkHttpClient
                        .Builder()
                        .connectionPool(okhttp3.ConnectionPool(8, 2, java.util.concurrent.TimeUnit.MINUTES))
                        .build()
                }
            val client = api.client(base = { base }).effectiveClient()
            val (maxIdle, keepAliveNanos) = poolSettings(client.connectionPool)
            org.junit.Assert.assertTrue(
                "Pool idle duration must stay below Cloudflare's 400 s edge limit: $keepAliveNanos ns",
                keepAliveNanos < 400.seconds.inWholeNanoseconds,
            )
            assertThat(keepAliveNanos).isEqualTo(300.seconds.inWholeNanoseconds)
            assertThat(maxIdle).isEqualTo(5)
            assertThat(client.retryOnConnectionFailure).isEqualTo(false)
            assertThat(client.connectionPool === base.connectionPool).isEqualTo(false)
            assertThat(poolSettings(base.connectionPool)).isEqualTo(8 to 120.seconds.inWholeNanoseconds)
        }

    @Test fun evictIdleConnectionsIsANoOpBeforeTheClientIsBuilt() {
        val client = api.client(base = { error("eviction must not build the client") })
        client.evictIdleConnections()
    }

    @Test fun evictIdleConnectionsPreventsReuseOfAnIdleConnection() =
        runTest(timeout = 60.seconds) {
            api.reply("GET", "/v1/users/me", body = "reply")
            val client = api.client()
            assertThat(client.request(endpoint) { it.decodeToString() }).isEqualTo("reply")
            assertThat(client.request(endpoint) { it.decodeToString() }).isEqualTo("reply")
            assertThat(api.requests[0].connectionIndex).isEqualTo(api.requests[1].connectionIndex)
            assertThat(client.effectiveClient().connectionPool.idleConnectionCount()).isEqualTo(1)

            client.evictIdleConnections()

            assertThat(client.effectiveClient().connectionPool.idleConnectionCount()).isEqualTo(0)
            assertThat(client.request(endpoint) { it.decodeToString() }).isEqualTo("reply")
            org.junit.Assert.assertNotEquals(api.requests[0].connectionIndex, api.requests[2].connectionIndex)
            assertThat(api.requests.map { it.exchangeIndex }).isEqualTo(listOf(0, 1, 0))
            assertThat(api.server.requestCount).isEqualTo(3)
        }

    private fun poolSettings(pool: okhttp3.ConnectionPool): Pair<Int, Long> {
        // OkHttp 5.5.1 exposes neither policy value publicly; inspect it without a wall-clock wait.
        val delegate =
            pool.javaClass
                .getDeclaredField("delegate")
                .apply { isAccessible = true }
                .get(pool)
        val maxIdle =
            delegate.javaClass
                .getDeclaredField(
                    "maxIdleConnections",
                ).apply { isAccessible = true }
                .getInt(delegate)
        val keepAlive =
            delegate.javaClass
                .getDeclaredField("keepAliveDurationNs")
                .apply {
                    isAccessible = true
                }.getLong(delegate)
        return maxIdle to keepAlive
    }

    @Test fun serviceUnavailableWithZeroRetryAfterIsNotRetried() =
        runTest(timeout = 60.seconds) {
            val attempts =
                java.util.concurrent.atomic
                    .AtomicInteger()
            api.route("POST", "/v1/terms/acceptance") {
                if (attempts.getAndIncrement() == 0) {
                    MockResponse
                        .Builder()
                        .code(503)
                        .addHeader("Retry-After", "0")
                        .build()
                } else {
                    response(204)
                }
            }
            val result =
                failure {
                    api.client().request(
                        ApiEndpoint(
                            "POST",
                            listOf("terms", "acceptance"),
                            body = """{"accepted":true,"version":"1.0"}""".encodeToByteArray(),
                        ),
                    ) { Unit }
                }
            org.junit.Assert.assertEquals("Result: $result", 1, api.server.requestCount)
            assertThat(result).isEqualTo(DataFailure.Server(503))
        }

    private val endpoint = ApiEndpoint(path = listOf("users", "me"))

    private suspend fun read(client: ApiClient = api.client()) = client.request(endpoint, SessionAccount.serializer())

    @Test fun fieldArguments() =
        runTest(timeout = 60.seconds) {
            api.reply("GET", "/v1/users/me", 422, json("errors/validation_failed.json"))
            assertThat(failure { read() }).isEqualTo(
                DataFailure.Validation(
                    mapOf(
                        "password" to
                            FieldMessage("Use no more than :max characters.", mapOf("max" to JsonPrimitive(255))),
                    ),
                    422,
                ),
            )
        }

    @Test fun malformedErrorsEndToEnd() =
        runTest(timeout = 60.seconds) {
            api.reply("GET", "/v1/users/me", 422, "<html>Error</html>")
            assertThat(failure { read() }).isEqualTo(DataFailure.Server(422))
        }

    @Test fun retryAfterEndToEnd() =
        runTest(timeout = 60.seconds) {
            api.route("GET", "/v1/users/me") {
                MockResponse
                    .Builder()
                    .code(429)
                    .body("""{"code":"throttled","message":"fake"}""")
                    .addHeader("Retry-After", "12")
                    .build()
            }
            assertThat(failure { read() }).isEqualTo(DataFailure.Throttled(12.0, 429))
        }

    @Test fun explicitCancellationIsNotAnOfflineFailure() =
        runTest(timeout = 60.seconds) {
            val started = CompletableDeferred<Unit>()
            val events = EventRecorder()
            api.route("GET", "/v1/users/me") {
                started.complete(Unit)
                MockResponse.Builder().onResponseStart(SocketEffect.Stall).build()
            }
            val task = async { read(api.client(base = { OkHttpClient.Builder().eventListener(events).build() })) }
            started.await()
            task.cancel()
            val canceled =
                try {
                    task.await()
                    false
                } catch (_: CancellationException) {
                    true
                }
            assertThat(canceled).isEqualTo(true)
            assertThat(events.canceled.get()).isEqualTo(1)
        }

    @Test fun deadlinesCancelUnderlyingTask3s() = deadline(3.seconds)

    @Test fun deadlinesCancelUnderlyingTask15s() = deadline(15.seconds)

    @Test fun deadlinesCancelUnderlyingTask90s() = deadline(90.seconds)

    private fun deadline(timeout: Duration) =
        runTest(timeout = 60.seconds) {
            val started = CompletableDeferred<Unit>()
            val events = EventRecorder()
            api.route("GET", "/v1/users/me") {
                started.complete(Unit)
                MockResponse.Builder().onResponseStart(SocketEffect.Stall).build()
            }
            val client =
                api.client(
                    base = { OkHttpClient.Builder().eventListener(events).build() },
                    deadline =
                        ApiDeadline {
                            assertThat(it).isEqualTo(timeout)
                            started.await()
                        },
                )
            assertThat(
                failure {
                    client.request(
                        ApiEndpoint(path = listOf("users", "me"), timeout = timeout),
                        SessionAccount.serializer(),
                    )
                },
            ).isEqualTo(DataFailure.Offline)
            assertThat(events.canceled.get()).isEqualTo(1)
            assertThat(api.server.requestCount).isEqualTo(1)
        }

    @Test fun noContentRequiresExactShape200() = noContent(200, "{}")

    @Test fun noContentRequiresExactShape201() = noContent(201, "{}")

    @Test fun noContentRequiresExactShape204WithBody() = noContent(204, "{}")

    @Test fun noContentRequiresExactShape204Empty() =
        runTest(timeout = 60.seconds) {
            api.reply("POST", "/v1/auth/logout", 204)
            api.client().requestNoContent(ApiEndpoint("POST", listOf("auth", "logout")))
        }

    private fun noContent(
        status: Int,
        body: String,
    ) = runTest(timeout = 60.seconds) {
        api.reply("POST", "/v1/auth/logout", status, body)
        assertThat(
            failure {
                api.client().requestNoContent(ApiEndpoint("POST", listOf("auth", "logout")))
            } is DataFailure.Server,
        ).isEqualTo(true)
    }

    @Test fun noCookieOrCacheReuseAndMissingBearer() =
        runTest(timeout = 60.seconds) {
            api.route("GET", "/v1/users/me") {
                MockResponse
                    .Builder()
                    .body(json("account/identity.json"))
                    .addHeader("Set-Cookie", "secret=fake")
                    .addHeader("Cache-Control", "max-age=3600")
                    .build()
            }
            val client = api.client()
            read(client)
            read(client)
            assertThat(api.server.requestCount).isEqualTo(2)
            assertThat(api.requests[1].headers["Cookie"]).isEqualTo(null)
            sanitized(client.effectiveClient())
            assertThat(failure { read(api.client(bearer = { null })) }).isEqualTo(DataFailure.Unauthorized(401))
            assertThat(failure { read(api.client(bearer = { "" })) }).isEqualTo(DataFailure.Unauthorized(401))
            assertThat(failure { read(api.client(bearer = { "bad\nvalue" })) }).isEqualTo(DataFailure.Unauthorized(401))
            assertThat(api.server.requestCount).isEqualTo(2)
        }

    @Test fun sanitizesInjectedConfiguration() =
        runTest(timeout = 60.seconds) {
            val cookies =
                object : CookieJar {
                    override fun saveFromResponse(
                        url: HttpUrl,
                        cookies: List<Cookie>,
                    ) = Unit

                    override fun loadForRequest(url: HttpUrl) =
                        listOf(
                            Cookie
                                .Builder()
                                .name("secret")
                                .value("fake")
                                .domain(url.host)
                                .build(),
                        )
                }
            Cache(folder.newFolder(), 4096).use { cache ->
                val base =
                    withContext(Dispatchers.IO) {
                        OkHttpClient
                            .Builder()
                            .cookieJar(cookies)
                            .cache(cache)
                            .addInterceptor {
                                it.proceed(
                                    it
                                        .request()
                                        .newBuilder()
                                        .header(
                                            "Authorization",
                                            "Bearer stale",
                                        ).header("Cookie", "secret=fake")
                                        .build(),
                                )
                            }.addNetworkInterceptor { it.proceed(it.request()) }
                            .build()
                    }
                val client = api.client(base = { base })
                sanitized(client.effectiveClient())
                assertThat(base.followRedirects).isEqualTo(true)
                assertThat(base.retryOnConnectionFailure).isEqualTo(true)
                assertThat(base.cookieJar === cookies).isEqualTo(true)
                assertThat(base.cache === cache).isEqualTo(true)
                assertThat(base.interceptors.size).isEqualTo(1)
                assertThat(base.networkInterceptors.size).isEqualTo(1)
                assertThat(base.readTimeoutMillis).isEqualTo(10000)
                api.reply("GET", "/v1/users/me", 200, json("account/identity.json"))
                client.request(
                    ApiEndpoint(path = listOf("users", "me"), access = ApiEndpoint.Access.PublicRoute),
                    SessionAccount.serializer(),
                )
                assertThat(api.requests.single().headers["Authorization"]).isEqualTo(null)
                assertThat(api.requests.single().headers["Cookie"]).isEqualTo(null)
            }
        }

    @Test fun headersAreExact() =
        runTest(timeout = 60.seconds) {
            api.reply("POST", "/v1/body", 200, "{}")
            api.reply("POST", "/v1/empty", 200, "{}")
            api.route("GET", "/v1/users/me") {
                MockResponse
                    .Builder()
                    .body("{}")
                    .addHeader("ETag", "x")
                    .build()
            }
            val client = api.client()
            client.request(ApiEndpoint("POST", listOf("body"), body = "{}".encodeToByteArray())) { Unit }
            client.request(ApiEndpoint("POST", listOf("empty"))) { Unit }
            client.request(endpoint) { Unit }
            client.request(endpoint) { Unit }
            assertThat(api.requests[0].headers.values("Accept")).isEqualTo(listOf("application/json"))
            assertThat(api.requests[0].headers["Content-Type"]).isEqualTo("application/json")
            assertThat(api.requests[0].headers["Authorization"]).isEqualTo("Bearer artificial-old-token")
            assertThat(api.requests[1].headers["Content-Type"]).isEqualTo(null)
            assertThat(api.requests[1].headers["Content-Length"]).isEqualTo("0")
            assertThat(api.requests[2].bodySize).isEqualTo(0L)
            assertThat(
                api.requests.any { r ->
                    r.headers.names().any {
                        it.contains("device", true) ||
                            it in setOf("Cookie", "Accept-Language", "If-None-Match")
                    }
                },
            ).isEqualTo(false)
        }

    @Test fun decodingFailureIsServerWithTheStatus() =
        runTest(timeout = 60.seconds) {
            api.reply("GET", "/v1/users/me", 200, """{"id":1}""")
            assertThat(failure { read() }).isEqualTo(DataFailure.Server(200))
        }

    @Test fun requestStatusMustMatchExpected() =
        runTest(timeout = 60.seconds) {
            api.reply("GET", "/v1/users/me", 201, "{}")
            assertThat(failure { read() }).isEqualTo(DataFailure.Server(201))
        }

    @Test fun decodingAndClientConstructionUseTheIoDispatcher() =
        runTest(timeout = 60.seconds) {
            val caller = Thread.currentThread()
            val client =
                api.client(base = {
                    assertThat(Thread.currentThread() === caller).isEqualTo(false)
                    OkHttpClient()
                })
            api.reply("GET", "/v1/users/me", 200, "{}")
            client.request(endpoint) { assertThat(Thread.currentThread() === caller).isEqualTo(false) }
        }

    @Test fun explicitCancellationInterruptsTheBodyRead() =
        runTest(timeout = 60.seconds) {
            val reading = CompletableDeferred<Unit>()
            val events = EventRecorder()
            val listener =
                object : okhttp3.EventListener() {
                    override fun canceled(call: okhttp3.Call) = events.canceled(call)

                    override fun responseBodyStart(call: okhttp3.Call) {
                        reading.complete(Unit)
                    }
                }
            api.route("GET", "/v1/users/me") {
                MockResponse
                    .Builder()
                    .body("partial body")
                    .onResponseBody(SocketEffect.Stall)
                    .build()
            }
            val task = async { read(api.client(base = { OkHttpClient.Builder().eventListener(listener).build() })) }
            reading.await()
            task.cancel()
            val canceled =
                try {
                    task.await()
                    false
                } catch (_: CancellationException) {
                    true
                }
            assertThat(canceled).isEqualTo(true)
            assertThat(events.canceled.get()).isEqualTo(1)
        }

    private fun sanitized(client: OkHttpClient) {
        assertThat(client.followRedirects).isEqualTo(false)
        assertThat(client.followSslRedirects).isEqualTo(false)
        assertThat(client.cookieJar === CookieJar.NO_COOKIES).isEqualTo(true)
        assertThat(client.cache).isEqualTo(null)
        assertThat(client.retryOnConnectionFailure).isEqualTo(false)
        assertThat(client.interceptors.isEmpty()).isEqualTo(true)
        assertThat(client.networkInterceptors).isEqualTo(listOf(ServiceUnavailableInterceptor))
        assertThat(client.connectTimeoutMillis).isEqualTo(0)
        assertThat(client.readTimeoutMillis).isEqualTo(0)
        assertThat(client.writeTimeoutMillis).isEqualTo(0)
        assertThat(client.callTimeoutMillis).isEqualTo(0)
    }

    @Test fun registrationValidationEnvelopePreservesVisibleFieldMessages() =
        runTest(timeout = 60.seconds) {
            val deadline = RecordingDeadline()
            api.reply(
                "POST",
                "/v1/auth/register",
                422,
                """{"code":"validation_failed","message":"Please fill this in.","errors":{"name":"Please fill this in.","email":"Please fill this in.","password":"Please fill this in."}}""",
            )
            val expected =
                mapOf(
                    "name" to FieldMessage("Please fill this in."),
                    "email" to FieldMessage("Please fill this in."),
                    "password" to FieldMessage("Please fill this in."),
                )
            assertThat(
                failure {
                    testSource(api.client(deadline = deadline)).signUp(registration())
                },
            ).isEqualTo(DataFailure.Validation(expected, 422))
            assertThat(deadline.durations.single()).isEqualTo(90.seconds)
        }

    @Test fun malformedSuccessEmptyObject() = malformedSuccess("{}")

    @Test fun malformedSuccessHtml() = malformedSuccess("<html>OK</html>")

    @Test fun malformedSuccessSessionWithoutAccount() = malformedSuccess("""{"kind":"session","token":"fake"}""")

    private fun malformedSuccess(body: String) =
        runTest(timeout = 60.seconds) {
            api.reply("POST", "/v1/auth/login", 200, body)
            assertThat(
                failure {
                    testSource(api.client()).signIn(
                        net.chronoflow.core.model.api
                            .PasswordSignInInput("x", "x", "x", "x"),
                    )
                },
            ).isEqualTo(DataFailure.Server(200))
        }

    @Test fun injectedAuthenticatorsCannotRetryPublicCalls() =
        runTest(timeout = 60.seconds) {
            val invocations =
                java.util.concurrent.atomic
                    .AtomicInteger()
            val authenticator =
                okhttp3.Authenticator { _, response ->
                    invocations.incrementAndGet()
                    response.request
                        .newBuilder()
                        .header("Authorization", "Bearer stale")
                        .build()
                }
            val base =
                withContext(Dispatchers.IO) {
                    OkHttpClient
                        .Builder()
                        .authenticator(authenticator)
                        .proxyAuthenticator(authenticator)
                        .build()
                }
            val client = api.client(base = { base })
            api.reply("GET", "/v1/users/me", 401, json("errors/unauthenticated.json"))
            assertThat(
                failure {
                    client.request(
                        ApiEndpoint(path = listOf("users", "me"), access = ApiEndpoint.Access.PublicRoute),
                    ) { Unit }
                },
            ).isEqualTo(DataFailure.Unauthorized(401))
            assertThat(invocations.get()).isEqualTo(0)
            assertThat(api.server.requestCount).isEqualTo(1)
            assertThat(api.requests.single().headers["Authorization"]).isEqualTo(null)
            assertThat(client.effectiveClient().authenticator === okhttp3.Authenticator.NONE).isEqualTo(true)
            assertThat(client.effectiveClient().proxyAuthenticator === okhttp3.Authenticator.NONE).isEqualTo(true)
            assertThat(base.authenticator === authenticator).isEqualTo(true)
            assertThat(base.proxyAuthenticator === authenticator).isEqualTo(true)
        }
}
