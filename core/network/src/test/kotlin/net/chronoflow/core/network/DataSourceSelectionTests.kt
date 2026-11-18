package net.chronoflow.core.network

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isSameInstanceAs
import org.junit.Test

class DataSourceSelectionTests {
    @Test fun defaultsAndIsolation() {
        val mock = DataSourceFactory(release = false, testing = false)
        val api = DataSourceFactory("api", release = false, testing = false)
        assertThat(mock.source).isEqualTo(DataSourceFactory.Source.Mock)
        assertThat(
            DataSourceFactory("api", release = true, testing = true).source,
        ).isEqualTo(DataSourceFactory.Source.Mock)
        assertThat(api.source).isEqualTo(DataSourceFactory.Source.Api)
        assertThat(mock.make({ "m" }, { "a" })).isEqualTo("m")
        assertThat(api.make({ "m" }, { "a" })).isEqualTo("a")
    }

    @Test fun rejectInvalidSettings() {
        invalidSource("")
        invalidSource("API")
        invalidSource("other")
        invalidSource("$(CHRONOFLOW_DATA_SOURCE)")
        invalidUrl("/v1")
        invalidUrl("https://")
        invalidUrl("file:///v1")
        invalidUrl("https://u:p@api.test/v1")
        invalidUrl("https://api.test/v1?q=x")
        invalidUrl("https://api.test/v1#x")
        invalidUrl("https://api.test/../v1")
        invalidUrl("https://api.test:0/v1")
        invalidUrl("https://api.test:70000/v1")
        invalidUrl("https://api.test/%2E%2E/v1")
        invalidUrl("HTTPS://api.test/v1")
        assertFailure { ApiBaseUrl.parse("http://localhost/v1", release = true) }
            .isSameInstanceAs(DataSourceConfigurationFailure.InvalidBaseUrl)
        assertThat(ApiBaseUrl.parse("http://localhost/v1", false).url.scheme).isEqualTo("http")
    }

    private fun invalidSource(value: String) {
        assertFailure { DataSourceFactory(value, release = false, testing = true) }
            .isSameInstanceAs(DataSourceConfigurationFailure.InvalidSource)
    }

    private fun invalidUrl(value: String) {
        assertFailure { ApiBaseUrl.parse(value, false) }.isSameInstanceAs(DataSourceConfigurationFailure.InvalidBaseUrl)
    }
}
