package net.chronoflow.core.network

sealed class DataSourceConfigurationFailure : Exception(null, null, false, false) {
    data object InvalidSource : DataSourceConfigurationFailure()

    data object InvalidBaseUrl : DataSourceConfigurationFailure()
}

class DataSourceFactory(
    setting: String = "mock",
    baseUrl: String = "https://api.chronoflow.net/v1",
    release: Boolean,
    testing: Boolean,
) {
    enum class Source(
        val raw: String,
    ) {
        Mock("mock"),
        Api("api"),
    }

    val source: Source
    val baseUrl: ApiBaseUrl

    init {
        val requested =
            Source.entries.firstOrNull { it.raw == setting }
                ?: throw DataSourceConfigurationFailure.InvalidSource
        this.baseUrl = ApiBaseUrl.parse(baseUrl, release)
        source = if (testing) Source.Mock else requested
    }

    fun <T> make(
        mock: () -> T,
        api: () -> T,
    ): T =
        when (source) {
            Source.Mock -> mock()
            Source.Api -> api()
        }
}
