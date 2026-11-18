package net.chronoflow.core.network

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.URI
import java.net.URISyntaxException

class ApiBaseUrl private constructor(
    val url: HttpUrl,
) {
    override fun equals(other: Any?): Boolean = other is ApiBaseUrl && url == other.url

    override fun hashCode(): Int = url.hashCode()

    companion object {
        fun parse(
            value: String,
            release: Boolean,
        ): ApiBaseUrl {
            val uri =
                try {
                    URI(value)
                } catch (_: URISyntaxException) {
                    throw DataSourceConfigurationFailure.InvalidBaseUrl
                }
            if (uri.scheme !in setOf("http", "https") || (release && uri.scheme != "https") ||
                uri.host.isNullOrEmpty() || (uri.port != -1 && uri.port !in 1..65535) ||
                uri.rawUserInfo != null || uri.rawQuery != null || uri.rawFragment != null ||
                uri.path
                    .orEmpty()
                    .split('/')
                    .any { it == "." || it == ".." }
            ) {
                throw DataSourceConfigurationFailure.InvalidBaseUrl
            }
            return ApiBaseUrl(value.toHttpUrlOrNull() ?: throw DataSourceConfigurationFailure.InvalidBaseUrl)
        }
    }
}
