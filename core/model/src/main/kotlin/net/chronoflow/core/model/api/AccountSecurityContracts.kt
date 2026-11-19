package net.chronoflow.core.model.api

import kotlinx.serialization.Serializable

@Serializable
data class ApiTwoFactorSetup(
    val secretKey: String,
    val otpauthUrl: String,
) {
    override fun toString(): String =
        "ApiTwoFactorSetup(secretKey=${redacted(secretKey)}, otpauthUrl=${redacted(otpauthUrl)})"
}

@Serializable
data class ApiRecoveryCodes(
    val recoveryCodes: List<String>,
) {
    override fun toString(): String = "ApiRecoveryCodes(recoveryCodes=${redacted(recoveryCodes)})"
}
