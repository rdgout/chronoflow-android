@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.core.network

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.network.support.MockApi
import net.chronoflow.core.network.support.RecordingDeadline
import net.chronoflow.core.network.support.failure
import net.chronoflow.core.network.support.json
import net.chronoflow.core.network.support.requestJson
import net.chronoflow.core.network.support.response
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class EndpointContractTest(
    private val label: String,
    private val row: EndpointRow,
) {
    @get:Rule val api = MockApi()

    @Test fun endpointContract() =
        runTest(timeout = 60.seconds) {
            val deadline = RecordingDeadline()
            val source = ApiDataSource(api.client(deadline = deadline))
            if (row.verb == "NONE") {
                assertThat(failure { row.call(source) }).isEqualTo(row.expected())
                assertThat(api.server.requestCount).isEqualTo(0)
                assertThat(deadline.durations.isEmpty()).isEqualTo(true)
            } else {
                api.reply(row.verb, "/v1/" + row.path, row.status, row.reply)
                assertThat(row.call(source)).isEqualTo(row.expected())
                assertThat(api.server.requestCount).isEqualTo(1)
                val request = api.requests.single()
                assertThat(request.method).isEqualTo(row.verb)
                assertThat(request.url.encodedPath).isEqualTo("/v1/" + row.path)
                assertThat(request.url.encodedQuery).isEqualTo(row.query)
                assertThat(request.headers.values("Accept")).isEqualTo(listOf("application/json"))
                assertThat(
                    request.headers["Authorization"],
                ).isEqualTo(if (row.public) null else "Bearer artificial-old-token")
                assertThat(request.headers["Cookie"]).isEqualTo(null)
                assertThat(request.headers["Accept-Language"]).isEqualTo(null)
                assertThat(request.headers["If-None-Match"]).isEqualTo(null)
                assertThat(request.headers["Content-Type"]).isEqualTo(
                    if (row.requestBody ==
                        null
                    ) {
                        null
                    } else {
                        "application/json"
                    },
                )
                assertThat(requestJson(request)).isEqualTo(row.requestBody)
                assertThat(deadline.durations).isEqualTo(listOf(row.seconds.seconds))
            }
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> = ENDPOINT_ROWS.map { arrayOf(it.label, it) }
    }
}
