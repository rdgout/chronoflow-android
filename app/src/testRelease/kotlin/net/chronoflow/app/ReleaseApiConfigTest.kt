package net.chronoflow.app

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isSameInstanceAs
import net.chronoflow.core.network.ApiBaseUrl
import net.chronoflow.core.network.DataSourceConfigurationFailure
import net.chronoflow.core.network.DataSourceFactory
import org.junit.Test

class ReleaseApiConfigTest {
    @Test fun releaseAlwaysUsesApiAndHttps() {
        assertThat(BuildConfig.DATA_SOURCE).isEqualTo("api")
        assertThat(BuildConfig.API_BASE_URL).isEqualTo("https://api.chronoflow.net/v1")
        assertThat(ApiBaseUrl.parse(BuildConfig.API_BASE_URL, release = true).url.scheme).isEqualTo("https")
        assertFailure { ApiBaseUrl.parse("http://api.chronoflow.net/v1", release = true) }
            .isSameInstanceAs(DataSourceConfigurationFailure.InvalidBaseUrl)
        val factory =
            DataSourceFactory(
                BuildConfig.DATA_SOURCE,
                BuildConfig.API_BASE_URL,
                release = true,
                testing = false,
            )
        assertThat(factory.source)
            .isEqualTo(DataSourceFactory.Source.Api)
    }
}
