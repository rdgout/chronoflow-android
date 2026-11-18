@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.core.network

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.SocketEffect
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.api.SessionAccount
import net.chronoflow.core.network.support.EventRecorder
import net.chronoflow.core.network.support.MockApi
import net.chronoflow.core.network.support.RecordingDeadline
import net.chronoflow.core.network.support.failure
import net.chronoflow.core.network.support.json
import net.chronoflow.core.network.support.requestJson
import net.chronoflow.core.network.support.response
import okhttp3.OkHttpClient
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.net.Proxy
import java.net.UnknownHostException
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class ApiClientTransportTests(
    private val mode: String,
) {
    @get:Rule val api = MockApi()

    @Test fun transportFailuresMapToOffline() =
        runTest(timeout = 60.seconds) {
            val client =
                when (mode) {
                    "disconnect" -> {
                        api.route(
                            "GET",
                            "/v1/users/me",
                        ) { MockResponse.Builder().onResponseStart(SocketEffect.CloseSocket()).build() }
                        api.client()
                    }

                    "body" -> {
                        api.route("GET", "/v1/users/me") {
                            MockResponse
                                .Builder()
                                .body("0123456789")
                                .setHeader("Content-Length", "100")
                                .onResponseEnd(SocketEffect.CloseSocket())
                                .build()
                        }
                        api.client()
                    }

                    "refused" -> {
                        val closed = mockwebserver3.MockWebServer()
                        closed.start()
                        val url = ApiBaseUrl.parse(closed.url("/v1").toString(), false)
                        closed.close()
                        ApiClient(
                            url,
                            bearer = { "artificial" },
                            deadline = ApiDeadline { kotlinx.coroutines.awaitCancellation() },
                        )
                    }

                    else -> {
                        api.client(base = {
                            OkHttpClient
                                .Builder()
                                .proxy(Proxy.NO_PROXY)
                                .dns { throw UnknownHostException("artificial") }
                                .build()
                        })
                    }
                }
            assertThat(
                failure { client.request(ApiEndpoint(path = listOf("users", "me")), SessionAccount.serializer()) },
            ).isEqualTo(DataFailure.Offline)
            assertThat(api.server.requestCount).isEqualTo(if (mode == "disconnect" || mode == "body") 1 else 0)
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Array<String>> =
            listOf(
                arrayOf("disconnect"),
                arrayOf("body"),
                arrayOf("refused"),
                arrayOf("dns"),
            )
    }
}
