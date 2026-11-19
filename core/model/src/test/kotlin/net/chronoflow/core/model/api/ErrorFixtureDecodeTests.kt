package net.chronoflow.core.model.api

import assertk.assertThat
import assertk.assertions.isEqualTo
import net.chronoflow.core.testing.Fixtures
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ErrorFixtureDecodeTests(
    private val stem: String,
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun rows(): List<String> =
            listOf(
                "conflict",
                "email_unverified",
                "forbidden",
                "http_error",
                "method_not_allowed",
                "not_found",
                "payload_too_large",
                "server_error",
                "throttled",
                "unauthenticated",
                "unavailable",
                "validation_failed",
                "wrong_credentials",
            )
    }

    @Test
    fun everyErrorFixtureDecodes() {
        val envelope =
            ApiJson.response.decodeFromString(
                ApiErrorEnvelope.serializer(),
                Fixtures.text("errors/$stem.json"),
            )
        val expected = if (stem == "wrong_credentials") "validation_failed" else stem
        assertThat(envelope.code).isEqualTo(expected)
    }
}
