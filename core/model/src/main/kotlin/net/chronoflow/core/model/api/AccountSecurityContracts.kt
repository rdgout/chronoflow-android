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

sealed interface AccountSecurityChange {
    data class RequestPasswordReset(
        val email: String,
    ) : AccountSecurityChange

    data class Password(
        val current: String,
        val password: String,
        val confirmation: String,
    ) : AccountSecurityChange {
        override fun toString(): String = "Password(***)"
    }

    data class EnableTwoFactor(
        val current: String,
    ) : AccountSecurityChange {
        override fun toString(): String = "EnableTwoFactor(***)"
    }

    data class ConfirmTwoFactor(
        val code: String,
    ) : AccountSecurityChange {
        override fun toString(): String = "ConfirmTwoFactor(***)"
    }

    data class DisableTwoFactor(
        val current: String,
    ) : AccountSecurityChange {
        override fun toString(): String = "DisableTwoFactor(***)"
    }

    data class RevealRecoveryCodes(
        val current: String,
    ) : AccountSecurityChange {
        override fun toString(): String = "RevealRecoveryCodes(***)"
    }

    data class RegenerateRecoveryCodes(
        val current: String,
    ) : AccountSecurityChange {
        override fun toString(): String = "RegenerateRecoveryCodes(***)"
    }

    data object ListPasskeys : AccountSecurityChange

    data class PasskeyCreationOptions(
        val current: String,
    ) : AccountSecurityChange {
        override fun toString(): String = "PasskeyCreationOptions(***)"
    }

    data class AddPasskey(
        val input: ApiPasskeyRegistrationInput,
    ) : AccountSecurityChange {
        override fun toString(): String = "AddPasskey(***)"
    }

    data class RemovePasskey(
        val id: Long,
        val current: String,
    ) : AccountSecurityChange {
        override fun toString(): String = "RemovePasskey(***)"
    }
}

sealed interface AccountSecurityResult {
    data object Changed : AccountSecurityResult

    data class Setup(
        val setup: ApiTwoFactorSetup,
    ) : AccountSecurityResult

    data class Codes(
        val codes: ApiRecoveryCodes,
    ) : AccountSecurityResult

    data class Snapshot(
        val snapshot: AccountSnapshot,
    ) : AccountSecurityResult

    data class Passkeys(
        val passkeys: List<ApiPasskey>,
    ) : AccountSecurityResult

    data class PasskeyCreation(
        val challenge: ApiPasskeyRegistrationChallenge,
    ) : AccountSecurityResult

    data class PasskeyAdded(
        val passkey: ApiPasskey,
    ) : AccountSecurityResult
}
