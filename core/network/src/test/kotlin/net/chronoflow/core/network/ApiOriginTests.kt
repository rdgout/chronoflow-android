package net.chronoflow.core.network

import assertk.assertThat
import assertk.assertions.isEqualTo
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Test

class ApiOriginTests {
    @Test fun ownsOnlyItsOriginAndPathPrefix() {
        val base = ApiBaseUrl.parse("https://api.test/v1", true)
        assertThat(base.owns("https://api.test/v1/trips".toHttpUrl())).isEqualTo(true)
        assertThat(base.owns("https://api.test/v2/trips".toHttpUrl())).isEqualTo(false)
        assertThat(base.owns("https://api.test/v1x/trips".toHttpUrl())).isEqualTo(false)
        assertThat(base.owns("https://other.test/v1/trips".toHttpUrl())).isEqualTo(false)
        assertThat(base.owns("https://api.test:444/v1/trips".toHttpUrl())).isEqualTo(false)
        assertThat(base.owns("http://api.test/v1/trips".toHttpUrl())).isEqualTo(false)
    }
}
