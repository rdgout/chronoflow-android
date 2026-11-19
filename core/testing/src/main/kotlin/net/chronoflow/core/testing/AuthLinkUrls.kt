package net.chronoflow.core.testing

object AuthLinkUrls {
    fun reset(
        token: String,
        email: String,
    ): String = "https://app.chronoflow.net/reset-password/${encode(token)}?email=${encode(email)}"

    fun verify(
        id: String,
        hash: String,
        expires: Long,
        signature: String,
    ): String =
        "https://app.chronoflow.net/email/verify/${encode(
            id,
        )}/${encode(hash)}?expires=$expires&signature=${encode(signature)}"

    private fun encode(value: String): String =
        buildString {
            value.toByteArray(Charsets.UTF_8).forEach { byte ->
                val code = byte.toInt() and 0xff
                val char = code.toChar()
                if (char in 'A'..'Z' || char in 'a'..'z' || char in '0'..'9' || char in "-._~") {
                    append(char)
                } else {
                    append('%')
                    append("0123456789ABCDEF"[code shr 4])
                    append("0123456789ABCDEF"[code and 15])
                }
            }
        }
}
