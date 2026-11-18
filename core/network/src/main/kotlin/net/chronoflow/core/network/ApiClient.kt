package net.chronoflow.core.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.api.ApiJson
import okhttp3.Authenticator
import okhttp3.Call
import okhttp3.Callback
import okhttp3.CookieJar
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.net.ProtocolException
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration

fun interface ApiDeadline {
    suspend fun await(timeout: Duration)

    companion object {
        val Real = ApiDeadline { delay(it) }
    }
}

class ApiClient(
    private val baseUrl: ApiBaseUrl,
    private val baseClient: () -> OkHttpClient = { OkHttpClient() },
    private val bearer: suspend () -> String? = { null },
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val deadline: ApiDeadline = ApiDeadline.Real,
    private val now: () -> Instant = { Instant.now() },
) {
    private val clientLock = Mutex()
    private var client: OkHttpClient? = null

    internal suspend fun effectiveClient(): OkHttpClient =
        withContext(io) {
            clientLock.withLock {
                client ?: baseClient()
                    .newBuilder()
                    .followRedirects(false)
                    .followSslRedirects(false)
                    .cookieJar(CookieJar.NO_COOKIES)
                    .authenticator(Authenticator.NONE)
                    .proxyAuthenticator(Authenticator.NONE)
                    .cache(null)
                    .retryOnConnectionFailure(false)
                    .connectTimeout(0, TimeUnit.MILLISECONDS)
                    .readTimeout(0, TimeUnit.MILLISECONDS)
                    .writeTimeout(0, TimeUnit.MILLISECONDS)
                    .callTimeout(0, TimeUnit.MILLISECONDS)
                    .apply {
                        interceptors().clear()
                        networkInterceptors().clear()
                    }.addNetworkInterceptor(ServiceUnavailableInterceptor)
                    .build()
                    .also { client = it }
            }
        }

    suspend fun <T> request(
        endpoint: ApiEndpoint,
        deserializer: DeserializationStrategy<T>,
        expectedStatus: Int = 200,
    ): T = request(endpoint, expectedStatus) { ApiJson.response.decodeFromString(deserializer, it.decodeToString()) }

    suspend fun <T> request(
        endpoint: ApiEndpoint,
        expectedStatus: Int = 200,
        decode: (ByteArray) -> T,
    ): T =
        withContext(io) {
            val reply = perform(endpoint)
            if (reply.code != expectedStatus) throw DataFailure.Server(reply.code)
            try {
                decode(reply.bytes)
            } catch (_: IllegalArgumentException) {
                throw DataFailure.Server(reply.code)
            }
        }

    suspend fun requestNoContent(endpoint: ApiEndpoint): Unit =
        withContext(io) {
            val reply = perform(endpoint)
            if (reply.code != 204 || reply.bytes.isNotEmpty()) throw DataFailure.Server(reply.code)
        }

    private class Reply(
        val code: Int,
        val bytes: ByteArray,
    )

    private suspend fun perform(endpoint: ApiEndpoint): Reply {
        currentCoroutineContext().ensureActive()
        val builder = Request.Builder().url(endpoint.url(baseUrl)).header("Accept", "application/json")
        val body =
            endpoint.body?.toRequestBody("application/json".toMediaType())
                ?: if (endpoint.method in
                    setOf("POST", "PUT", "PATCH", "DELETE")
                ) {
                    ByteArray(0).toRequestBody(null)
                } else {
                    null
                }
        builder.method(endpoint.method, body)
        if (endpoint.access == ApiEndpoint.Access.Authenticated) {
            val token = bearer()
            if (token.isNullOrEmpty()) throw DataFailure.Unauthorized(401)
            try {
                builder.header("Authorization", "Bearer $token")
            } catch (
                _: IllegalArgumentException,
            ) {
                throw DataFailure.Unauthorized(401)
            }
        }
        currentCoroutineContext().ensureActive()
        val call = effectiveClient().newCall(builder.build())
        return coroutineScope {
            val timedOut = AtomicBoolean(false)
            val work =
                async {
                    val response =
                        try {
                            call.await()
                        } catch (
                            _: ProtocolException,
                        ) {
                            throw DataFailure.Server(null)
                        } catch (_: IOException) {
                            throw DataFailure.Offline
                        }
                    response.use {
                        // Cancellation must also interrupt a blocking body read after onResponse has resumed.
                        val workContext = currentCoroutineContext()
                        val cancellation =
                            launch(Dispatchers.Unconfined) {
                                try {
                                    kotlinx.coroutines.awaitCancellation()
                                } finally {
                                    if (!workContext.isActive) call.cancel()
                                }
                            }
                        try {
                            val bytes =
                                try {
                                    response.body.bytes()
                                } catch (_: IOException) {
                                    currentCoroutineContext().ensureActive()
                                    throw DataFailure.Offline
                                }
                            if (response.priorResponse != null || !baseUrl.owns(response.request.url)) {
                                throw DataFailure.Server(response.code)
                            }
                            if (response.code !in 200..299) {
                                throw ApiErrorMapper.map(response.code, bytes, response.header("Retry-After"), now())
                            }
                            Reply(response.code, bytes)
                        } finally {
                            cancellation.cancel()
                        }
                    }
                }
            val timer =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    deadline.await(endpoint.timeout)
                    timedOut.set(true)
                    call.cancel()
                    work.cancel()
                }
            try {
                work.await()
            } catch (e: CancellationException) {
                currentCoroutineContext().ensureActive()
                if (timedOut.get()) throw DataFailure.Offline else throw e
            } finally {
                timer.cancel()
            }
        }
    }
}

internal object ServiceUnavailableInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        // OkHttp retries 503 + Retry-After: 0 even when connection retries are disabled.
        return if (response.code == 503) response.newBuilder().removeHeader("Retry-After").build() else response
    }
}

private suspend fun Call.await(): Response =
    suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        enqueue(
            object : Callback {
                override fun onFailure(
                    call: Call,
                    e: IOException,
                ) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(
                    call: Call,
                    response: Response,
                ) {
                    continuation.resume(response) { _, value, _ -> value.close() }
                }
            },
        )
    }
