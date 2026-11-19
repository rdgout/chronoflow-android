package net.chronoflow.core.network

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.time.Instant

@RunWith(Parameterized::class)
class ApiErrorFixtureMappingTests(
    private val name: String,
    private val status: Int,
    private val expected: DataFailure,
) {
    @Test fun everyErrorFixtureMapsToOneFailure() {
        val body =
            net.chronoflow.core.testing.Fixtures
                .bytes("errors/$name.json")
        assertThat(ApiErrorMapper.map(status, body, null, Instant.EPOCH)).isEqualTo(expected)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Array<Any?>> =
            listOf(
                arrayOf("conflict", 409, DataFailure.Conflict(409)),
                arrayOf("email_unverified", 403, DataFailure.EmailUnverified(403)),
                arrayOf("forbidden", 403, DataFailure.Server(403)),
                arrayOf("http_error", 418, DataFailure.Server(418)),
                arrayOf("method_not_allowed", 405, DataFailure.Server(405)),
                arrayOf("not_found", 404, DataFailure.Server(404)),
                arrayOf("payload_too_large", 413, DataFailure.Server(413)),
                arrayOf("server_error", 500, DataFailure.Server(500)),
                arrayOf("throttled", 429, DataFailure.Throttled(37.0, 429)),
                arrayOf("unauthenticated", 401, DataFailure.Unauthorized(401)),
                arrayOf("unavailable", 503, DataFailure.Server(503)),
                arrayOf(
                    "validation_failed",
                    422,
                    DataFailure.Validation(
                        mapOf(
                            "password" to
                                FieldMessage(
                                    "Use no more than :max characters.",
                                    mapOf("max" to JsonPrimitive(255)),
                                ),
                        ),
                        422,
                    ),
                ),
                arrayOf(
                    "wrong_credentials",
                    422,
                    DataFailure.Validation(
                        mapOf("email" to FieldMessage("These sign-in details don't match our records.")),
                        422,
                    ),
                ),
            )
    }
}
