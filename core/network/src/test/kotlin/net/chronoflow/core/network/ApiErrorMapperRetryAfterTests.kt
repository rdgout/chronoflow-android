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
class ApiErrorMapperRetryAfterTests(
    private val label: String,
    private val body: String,
    private val header: String?,
    private val expected: Double,
    private val epoch: Long,
) {
    @Test fun retryAfter() {
        assertThat(ApiErrorMapper.map(429, body.encodeToByteArray(), header, Instant.ofEpochSecond(epoch)))
            .isEqualTo(DataFailure.Throttled(expected, 429))
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Array<Any?>> =
            listOf(
                arrayOf(
                    "body",
                    "{\"retryAfterSeconds\":37,\"code\":\"throttled\",\"message\":\"fake\"}",
                    "12",
                    37.0,
                    0L,
                ),
                arrayOf("header", "{\"code\":\"throttled\",\"message\":\"fake\"}", "12", 12.0, 0L),
                arrayOf("date", "{}", "Thu, 01 Jan 1970 00:02:00 GMT", 120.0, 0L),
                arrayOf("invalid", "{}", "bad", 60.0, 0L),
                arrayOf("negative", "{\"retryAfterSeconds\":-1}", "23", 23.0, 0L),
                arrayOf("string", "{\"retryAfterSeconds\":\"37\"}", "12", 12.0, 0L),
                arrayOf("bare429", "not json", "17", 17.0, 0L),
                arrayOf("no code", "{\"retryAfterSeconds\":23}", null, 23.0, 0L),
                arrayOf("empty", "", null, 60.0, 0L),
                arrayOf("past", "{}", "Thu, 01 Jan 1970 00:00:00 GMT", 0.0, 120L),
            )
    }
}
