@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.core.network

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.api.SessionAccount
import net.chronoflow.core.network.support.failure
import okhttp3.OkHttpClient
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.ConnectException
import java.net.InetAddress
import java.net.Socket
import java.net.SocketAddress
import javax.net.SocketFactory
import java.net.Proxy
import java.net.UnknownHostException
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class ApiClientTransportTests(
    private val mode: String,
) {
    @Test fun transportFailuresMapToOffline() =
        runTest(timeout = 60.seconds) {
            val sockets = ScriptedTransportSocketFactory(mode)
            val client = ApiClient(
                ApiBaseUrl.parse("http://transport.test:8080/v1", false),
                baseClient = {
                    OkHttpClient.Builder().proxy(Proxy.NO_PROXY).socketFactory(sockets).apply {
                        dns { if (mode == "dns") throw UnknownHostException("artificial") else listOf(InetAddress.getLoopbackAddress()) }
                    }.build()
                },
                bearer = { "artificial" },
                deadline = ApiDeadline { kotlinx.coroutines.awaitCancellation() },
            )
            assertThat(
                failure { client.request(ApiEndpoint(path = listOf("users", "me")), SessionAccount.serializer()) },
            ).isEqualTo(DataFailure.Offline)
            assertThat(sockets.attempts).isEqualTo(if (mode == "dns") 0 else 1)
            assertThat(sockets.requests.toString(Charsets.UTF_8).startsWith("GET /v1/users/me HTTP/1.1\r\n")).isEqualTo(mode == "disconnect" || mode == "body")
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

/** Exercises OkHttp's real HTTP codec without the scheduling of a peer socket or ephemeral port reuse. */
private class ScriptedTransportSocketFactory(private val mode: String) : SocketFactory() {
    var attempts = 0
        private set
    val requests = ByteArrayOutputStream()

    override fun createSocket(): Socket = object : Socket() {
        override fun connect(endpoint: SocketAddress?, timeout: Int) {
            attempts++
            if (mode == "refused") throw ConnectException("artificial")
        }

        override fun getInputStream(): InputStream = ByteArrayInputStream(
            if (mode == "body") {
                "HTTP/1.1 200 OK\r\nContent-Length: 100\r\n\r\n0123456789".toByteArray()
            } else {
                byteArrayOf()
            },
        )

        override fun getOutputStream(): OutputStream = requests

        override fun setSoTimeout(timeout: Int) = Unit

        override fun getSoTimeout(): Int = 0

        override fun setTcpNoDelay(on: Boolean) = Unit
    }

    override fun createSocket(host: String?, port: Int): Socket = error("unexpected socket overload")

    override fun createSocket(host: String?, port: Int, localHost: InetAddress?, localPort: Int): Socket = error("unexpected socket overload")

    override fun createSocket(host: InetAddress?, port: Int): Socket = error("unexpected socket overload")

    override fun createSocket(host: InetAddress?, port: Int, localHost: InetAddress?, localPort: Int): Socket = error("unexpected socket overload")
}
