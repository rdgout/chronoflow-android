@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.core.network

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.api.SessionAccount
import net.chronoflow.core.network.support.EventRecorder
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
class ApiClientRedirectTests(
    private val label: String,
    private val status: Int,
    private val cross: Boolean,
) {
    @get:Rule val api = MockApi()

    @get:Rule val foreign = MockApi()

    @Test fun refusesEveryRedirect() =
        runTest(timeout = 60.seconds) {
            api.route("GET", "/v1/users/me") {
                val target = if (cross) foreign.server.url("/v1/next").toString() else "/v1/next"
                mockwebserver3.MockResponse
                    .Builder()
                    .code(status)
                    .addHeader("Location", target)
                    .addHeader("Set-Cookie", "secret=fake; Path=/")
                    .build()
            }
            assertThat(
                failure {
                    api.client().request(
                        ApiEndpoint(path = listOf("users", "me")),
                        SessionAccount.serializer(),
                    )
                },
            ).isEqualTo(DataFailure.Server(status))
            assertThat(api.server.requestCount).isEqualTo(1)
            assertThat(foreign.server.requestCount).isEqualTo(0)
            assertThat(api.requests.single().headers["Authorization"]).isEqualTo("Bearer artificial-old-token")
            assertThat(api.requests.single().headers["Cookie"]).isEqualTo(null)
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> =
            listOf(
                arrayOf("301 False", 301, false),
                arrayOf("301 True", 301, true),
                arrayOf("302 False", 302, false),
                arrayOf("302 True", 302, true),
                arrayOf("307 False", 307, false),
                arrayOf("307 True", 307, true),
                arrayOf("308 False", 308, false),
                arrayOf("308 True", 308, true),
            )
    }
}
