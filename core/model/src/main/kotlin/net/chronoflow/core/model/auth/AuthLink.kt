package net.chronoflow.core.model.auth

import net.chronoflow.core.model.api.SignedVerificationInput
import java.net.URI
import java.net.URISyntaxException

data class ResetPasswordLink(
    val token: String,
    val email: String,
) {
    val isValid: Boolean get() = token.isNotBlank() && email.isNotBlank()

    override fun toString(): String = "ResetPasswordLink(token=<redacted>, email=<redacted>)"
}

sealed interface AuthLink {
    data class Reset(
        val link: ResetPasswordLink,
    ) : AuthLink

    data class Verify(
        val input: SignedVerificationInput?,
    ) : AuthLink

    companion object {
        private val uuid = Regex("^[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}$")

        fun parse(url: String): AuthLink? {
            val uri =
                try {
                    URI(url)
                } catch (_: URISyntaxException) {
                    return null
                }
            if (uri.scheme != "https" || uri.host != "app.chronoflow.net" || uri.port != -1 ||
                uri.rawUserInfo != null || uri.rawFragment != null
            ) {
                return null
            }
            val path = (uri.rawPath ?: "").split("/")
            val items =
                uri.rawQuery
                    ?.takeIf { it.isNotEmpty() }
                    ?.split("&")
                    ?.map { item ->
                        val separator = item.indexOf('=')
                        val name = if (separator < 0) item else item.substring(0, separator)
                        val value = if (separator < 0) null else percentDecodeOrNull(item.substring(separator + 1))
                        percentDecodeOrNull(name) to value
                    }.orEmpty()
            if (path.size >= 3 && path[1] == "email" && path[2] == "verify") {
                val expires = items.filter { it.first == "expires" }
                val signature = items.filter { it.first == "signature" }
                if (path.size != 5 || items.size != 2 || expires.size != 1 || signature.size != 1) {
                    return Verify(null)
                }
                val id = percentDecodeOrNull(path[3]) ?: return Verify(null)
                val hash = percentDecodeOrNull(path[4]) ?: return Verify(null)
                val seconds = expires[0].second ?: return Verify(null)
                val signed = signature[0].second ?: return Verify(null)
                if (!uuid.matches(id) || !hex(hash, 40) || seconds.isEmpty() ||
                    !seconds.all { it in '0'..'9' } || !hex(signed, 64)
                ) {
                    return Verify(null)
                }
                val expiration = seconds.toLongOrNull() ?: return Verify(null)
                return Verify(SignedVerificationInput(id, hash, expiration, signed))
            }
            if (path.size < 2 || path[1] != "reset-password") return null
            val emails = items.filter { it.first == "email" }
            val valid = path.size == 3 && path[2].isNotEmpty() && emails.size == 1 && items.size == 1
            return Reset(
                ResetPasswordLink(
                    token = if (valid) percentDecodeOrNull(path[2]) ?: "" else "",
                    email = if (emails.size == 1) emails[0].second ?: "" else "",
                ),
            )
        }

        private fun hex(
            value: String,
            count: Int,
        ): Boolean = value.length == count && value.all { it in '0'..'9' || it in 'A'..'F' || it in 'a'..'f' }
    }
}
