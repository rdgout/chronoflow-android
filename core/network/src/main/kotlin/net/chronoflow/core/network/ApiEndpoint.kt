package net.chronoflow.core.network

import net.chronoflow.core.model.DataFailure
import okhttp3.HttpUrl
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class ApiEndpoint(
    val method: String = "GET",
    val path: List<String>,
    val access: Access = Access.Authenticated,
    val body: ByteArray? = null,
    val timeout: Duration = ApiTimeouts.Default,
    val query: Map<String, String> = emptyMap(),
) {
    enum class Access { PublicRoute, Authenticated }

    fun url(base: ApiBaseUrl): HttpUrl {
        val builder =
            base.url.newBuilder().encodedPath(
                base.url.encodedPath
                    .trimEnd('/')
                    .ifEmpty { "/" },
            )
        for (part in path) {
            if (part.isEmpty() || part == "." || part == "..") throw DataFailure.Server(null)
            builder.addEncodedPathSegment(encode(part))
        }
        query.toSortedMap().forEach { (key, value) -> builder.addQueryParameter(key, value) }
        return builder.build()
    }

    override fun toString(): String = "$method /${path.joinToString("/") }"

    private fun encode(part: String): String =
        buildString {
            for (byte in part.encodeToByteArray()) {
                val value = byte.toInt() and 255
                if (value in 65..90 || value in 97..122 || value in 48..57 || value in listOf(45, 95, 126)) {
                    append(value.toChar())
                } else {
                    append('%')
                    append("0123456789ABCDEF"[value shr 4])
                    append("0123456789ABCDEF"[value and 15])
                }
            }
        }
}

object ApiTimeouts {
    val Default = 15.seconds
    val Generation = 90.seconds
    val Logout = 3.seconds
}
