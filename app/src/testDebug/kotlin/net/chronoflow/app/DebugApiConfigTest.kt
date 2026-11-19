package net.chronoflow.app

import assertk.assertThat
import assertk.assertions.isEqualTo
import net.chronoflow.core.network.DataSourceFactory
import org.junit.Test

class DebugApiConfigTest {
    @Test fun debugDefaultsToMockAtTheProductionBaseUrl() {
        assertThat(BuildConfig.DATA_SOURCE).isEqualTo("mock")
        assertThat(BuildConfig.API_BASE_URL).isEqualTo("https://api.chronoflow.net/v1")
        assertThat(
            DataSourceFactory(
                BuildConfig.DATA_SOURCE,
                BuildConfig.API_BASE_URL,
                release = false,
                testing = false,
            ).source,
        ).isEqualTo(DataSourceFactory.Source.Mock)
    }
}
