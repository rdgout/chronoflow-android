package net.chronoflow.feature.settings.security

object RecoveryCodesDocument {
    fun bytes(codes: List<String>): ByteArray = codes.joinToString("\n").toByteArray(Charsets.UTF_8)
}
