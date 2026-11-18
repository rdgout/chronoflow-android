package net.chronoflow.core.model.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class ApiPasskeyChallenge(
    val challengeId: String,
    val options: ApiPasskeyRequestOptions,
) {
    override fun toString(): String = "ApiPasskeyChallenge(challengeId=${redacted(challengeId)}, options=$options)"
}

@Serializable
data class ApiPasskeyRequestOptions(
    val challenge: String,
    val rpId: String,
    val timeout: Int,
    val userVerification: UserVerification,
    val allowCredentials: List<EmptyCredential>,
)

@Serializable
data class ApiPasskeyLoginInput(
    val challengeId: String,
    val credential: ApiPasskeyAssertion,
    @SerialName("device_id") val deviceID: String,
    @SerialName("device_name") val deviceName: String,
    @SerialName("draft_token") val draftToken: String? = null,
    @SerialName("guest_secret") val guestSecret: String? = null,
) {
    override fun toString(): String =
        "ApiPasskeyLoginInput(challengeId=${redacted(challengeId)}, credential=$credential, deviceID=$deviceID, " +
            "deviceName=$deviceName, draftToken=${redacted(draftToken)}, guestSecret=${redacted(guestSecret)})"
}

@Serializable
data class ApiPasskeyAssertion(
    val id: String,
    val rawId: String,
    val type: CredentialType,
    val response: ApiPasskeyAssertionResponse,
) {
    override fun toString(): String = "ApiPasskeyAssertion(***)"
}

@Serializable
data class ApiPasskeyAssertionResponse(
    val clientDataJSON: String,
    val authenticatorData: String,
    val signature: String,
    val userHandle: String? = null,
) {
    override fun toString(): String = "ApiPasskeyAssertionResponse(***)"
}

@Serializable
enum class UserVerification {
    @SerialName("required")
    Required,
}

@Serializable
enum class CredentialType {
    @SerialName("public-key")
    PublicKey,
}

/** Uninhabited: `allowCredentials` decodes only when empty. */
@Serializable(with = EmptyCredential.Serializer::class)
sealed interface EmptyCredential {
    object Serializer : KSerializer<EmptyCredential> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("net.chronoflow.core.model.api.EmptyCredential", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): EmptyCredential =
            throw SerializationException("allowCredentials must be empty")

        override fun serialize(
            encoder: Encoder,
            value: EmptyCredential,
        ) = throw SerializationException("EmptyCredential has no values")
    }
}
