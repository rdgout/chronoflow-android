package net.chronoflow.core.network

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import net.chronoflow.core.model.DataFailure
import org.junit.Test

class ApiEndpointTests {
    private val base = ApiBaseUrl.parse("https://example.invalid/v1/", false)

    @Test fun encodesEverySegment() {
        assertThat(ApiEndpoint(path = listOf("trips", "a/b?# %")).url(base).toString())
            .isEqualTo("https://example.invalid/v1/trips/a%2Fb%3F%23%20%25")
        assertThat(ApiEndpoint(path = listOf("trips", "a.b", "é")).url(base).encodedPath)
            .isEqualTo("/v1/trips/a%2Eb/%C3%A9")
        assertThat(ApiEndpoint(path = listOf("trips")).url(ApiBaseUrl.parse("https://example.invalid/v1", false)))
            .isEqualTo(ApiEndpoint(path = listOf("trips")).url(base))
    }

    @Test fun refusesEmptyAndDotSegments() {
        invalid("")
        invalid(".")
        invalid("..")
    }

    @Test fun sortsQueryAndEncodesPlus() {
        val url =
            ApiEndpoint(
                path = listOf("airports"),
                query =
                    mapOf(
                        "query" to "São Paulo & city+airport",
                        "limit" to "7",
                    ),
            ).url(base)
        assertThat(url.encodedQuery).isEqualTo("limit=7&query=S%C3%A3o%20Paulo%20%26%20city%2Bairport")
        assertThat(url.queryParameter("query")).isEqualTo("São Paulo & city+airport")
    }

    @Test fun toStringNeverPrintsTheBody() {
        assertThat(ApiEndpoint(path = listOf("auth"), body = "artificial-password".encodeToByteArray()).toString())
            .isEqualTo("GET /auth")
    }

    private fun invalid(value: String) {
        assertFailure { ApiEndpoint(path = listOf("trips", value)).url(base) }.isInstanceOf(DataFailure.Server::class)
    }
}
