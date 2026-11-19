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
class ApiErrorMapperMalformedTests(
    private val label: String,
    private val body: String,
) {
    @Test fun malformedErrors() {
        assertThat(
            ApiErrorMapper.map(422, body.encodeToByteArray(), null, Instant.EPOCH),
        ).isEqualTo(DataFailure.Server(422))
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Array<Any?>> =
            listOf(
                arrayOf("0", "{}"),
                arrayOf("1", "<html>Error</html>"),
                arrayOf("2", "{\"code\": \"validation_failed\", \"message\": \"bad\"}"),
                arrayOf("3", "{\"code\": \"validation_failed\", \"message\": \"bad\", \"errors\": {}}"),
                arrayOf(
                    "4",
                    "{\"code\": \"validation_failed\", \"message\": \"bad\", \"errors\": {\"a\": []}}",
                ),
                arrayOf(
                    "5",
                    "{\"code\": \"validation_failed\", \"message\": \"bad\", \"errors\": {\"a\": \"x\"}, \"args\": {\"a\": {\"max\": null}}}",
                ),
                arrayOf(
                    "6",
                    "{\"code\": \"validation_failed\", \"message\": \"bad\", \"errors\": {\"\": \"x\"}}",
                ),
                arrayOf(
                    "7",
                    "{\"code\": \"validation_failed\", \"message\": \"bad\", \"errors\": {\"a\": \"\"}}",
                ),
                arrayOf(
                    "8",
                    "{\"code\": \"validation_failed\", \"message\": \"bad\", \"errors\": {\"a\": \"x\"}, \"args\": {\"a\": {\"max\": true}}}",
                ),
                arrayOf(
                    "9",
                    "{\"code\": \"validation_failed\", \"message\": \"bad\", \"errors\": {\"a\": \"x\"}, \"args\": {\"b\": {\"max\": null}}}",
                ),
            )
    }
}
