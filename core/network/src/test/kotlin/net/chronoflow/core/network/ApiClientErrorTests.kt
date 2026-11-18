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
class ApiClientErrorTests(
    private val code: String,
    private val status: Int,
    private val expected: DataFailure,
) {
    @get:Rule val api = MockApi()

    @Test fun errors() =
        runTest(timeout = 60.seconds) {
            api.reply("GET", "/v1/users/me", status, json("errors/$code.json"))
            val result =
                failure { api.client().request(ApiEndpoint(path = listOf("users", "me")), SessionAccount.serializer()) }
            assertThat(result).isEqualTo(expected)
            assertThat(result.status).isEqualTo(status)
            assertThat(api.server.requestCount).isEqualTo(1)
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> =
            listOf(
                arrayOf("unauthenticated", 401, DataFailure.Unauthorized(401)),
                arrayOf("email_unverified", 403, DataFailure.EmailUnverified(403)),
                arrayOf("conflict", 409, DataFailure.Conflict(409)),
                arrayOf("forbidden", 403, DataFailure.Server(403)),
                arrayOf("not_found", 404, DataFailure.Server(404)),
                arrayOf("method_not_allowed", 405, DataFailure.Server(405)),
                arrayOf("payload_too_large", 413, DataFailure.Server(413)),
                arrayOf("unavailable", 503, DataFailure.Server(503)),
                arrayOf("server_error", 500, DataFailure.Server(500)),
                arrayOf("http_error", 418, DataFailure.Server(418)),
            )
    }
}
