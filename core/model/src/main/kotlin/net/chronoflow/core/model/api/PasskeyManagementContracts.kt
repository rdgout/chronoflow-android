package net.chronoflow.core.model.api

import kotlinx.serialization.Serializable

// Phase 20 in chronoflow-new docs/ui-plan/phase-20-mobile-api-passkey-management.md (branch
// feat/mobile-api-passkey-management). Not merged or deployed yet. The DTOs are hand-written until the contracts
// generator covers them.
@Serializable
data class ApiPasskey(
    // The route allows 18 digits.
    val id: Long,
    val name: String,
    val authenticator: String?,
    val createdAt: String,
    val lastUsedAt: String?,
)

@Serializable
data class ApiPasskeyList(
    val passkeys: List<ApiPasskey>,
)

@Serializable
data class ApiPasskeyRegistrationChallenge(
    val challengeId: String,
    val options: ApiPasskeyCreationOptions,
) {
    override fun toString(): String =
        "ApiPasskeyRegistrationChallenge(challengeId=${redacted(challengeId)}, options=$options)"
}

@Serializable
data class ApiPasskeyCreationOptions(
    val challenge: String,
    val timeout: Int,
    val rp: ApiPasskeyRelyingParty,
    val user: ApiPasskeyUserEntity,
    val pubKeyCredParams: List<ApiPasskeyCredentialParameters>,
    val authenticatorSelection: ApiPasskeyAuthenticatorSelection,
    val attestation: String,
    val excludeCredentials: List<ApiPasskeyCredentialDescriptor>,
)

@Serializable
data class ApiPasskeyRelyingParty(
    val id: String,
    val name: String,
)

@Serializable
data class ApiPasskeyUserEntity(
    val id: String,
    val name: String,
    val displayName: String,
)

@Serializable
data class ApiPasskeyCredentialParameters(
    val type: CredentialType,
    val alg: Int,
)

@Serializable
data class ApiPasskeyAuthenticatorSelection(
    val requireResidentKey: Boolean,
    val userVerification: String,
    val residentKey: String,
)

@Serializable
data class ApiPasskeyCredentialDescriptor(
    val type: CredentialType,
    val id: String,
)

@Serializable
data class ApiPasskeyAttestation(
    val id: String,
    val rawId: String,
    val type: CredentialType,
    val response: ApiPasskeyAttestationResponse,
)

@Serializable
data class ApiPasskeyAttestationResponse(
    val clientDataJSON: String,
    val attestationObject: String,
    val transports: List<String>? = null,
)

@Serializable
data class ApiPasskeyRegistrationInput(
    val challengeId: String,
    val name: String,
    val credential: ApiPasskeyAttestation,
) {
    override fun toString(): String =
        "ApiPasskeyRegistrationInput(challengeId=${redacted(challengeId)}, name=$name, credential=$credential)"
}
