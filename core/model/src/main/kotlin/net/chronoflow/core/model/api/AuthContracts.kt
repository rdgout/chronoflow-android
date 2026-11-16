package net.chronoflow.core.model.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.contracts.ObjectSerializer
import net.chronoflow.core.model.contracts.optionalStringAt
import net.chronoflow.core.model.contracts.stringAt

@Serializable
data class PasswordSignInInput(
    val email: String,
    val password: String,
    @SerialName("device_id") val deviceID: String,
    @SerialName("device_name") val deviceName: String,
    @SerialName("draft_token") val draftToken: String? = null,
    @SerialName("guest_secret") val guestSecret: String? = null,
) {
    override fun toString(): String =
        "PasswordSignInInput(email=$email, password=${redacted(
            password,
        )}, deviceID=$deviceID, deviceName=$deviceName, " +
            "draftToken=${redacted(draftToken)}, guestSecret=${redacted(guestSecret)})"
}

@Serializable
data class RegisterInput(
    val name: String,
    val email: String,
    val password: String,
    @SerialName("password_confirmation") val passwordConfirmation: String,
    @SerialName("device_id") val deviceID: String,
    @SerialName("device_name") val deviceName: String,
    val language: AuthLanguage? = null,
    @SerialName("draft_token") val draftToken: String? = null,
    @SerialName("input_revision") val inputRevision: Int? = null,
    @SerialName("review_revision") val reviewRevision: Int? = null,
    @SerialName("guest_secret") val guestSecret: String? = null,
) {
    override fun toString(): String =
        "RegisterInput(name=$name, email=$email, password=${redacted(password)}, " +
            "passwordConfirmation=${redacted(passwordConfirmation)}, deviceID=$deviceID, deviceName=$deviceName, " +
            "language=$language, draftToken=${redacted(draftToken)}, inputRevision=$inputRevision, " +
            "reviewRevision=$reviewRevision, guestSecret=${redacted(guestSecret)})"
}

@Serializable
data class ResetPasswordInput(
    val token: String,
    val email: String,
    val password: String,
    @SerialName("password_confirmation") val passwordConfirmation: String,
) {
    override fun toString(): String =
        "ResetPasswordInput(token=${redacted(token)}, email=$email, password=${redacted(password)}, " +
            "passwordConfirmation=${redacted(passwordConfirmation)})"
}

@Serializable
data class SignedVerificationInput(
    val id: String,
    val hash: String,
    // A Unix time: 32 bits run out in 2038.
    val expires: Long,
    val signature: String,
) {
    override fun toString(): String =
        "SignedVerificationInput(id=$id, hash=${redacted(hash)}, expires=$expires, signature=${redacted(signature)})"
}

@Serializable
data class EmailVerificationStatus(
    val email: String,
    val hasVerifiedEmail: Boolean,
    val resendAfterSeconds: Int,
)

@Serializable
enum class AuthLanguage {
    @SerialName("en")
    En,

    @SerialName("nl")
    Nl,
}

@Serializable(with = SignInResult.Serializer::class)
sealed interface SignInResult {
    data class Session(
        val result: SessionResult,
    ) : SignInResult

    data class TwoFactor(
        val challengeId: String,
    ) : SignInResult {
        override fun toString(): String = "TwoFactor(challengeId=${redacted(challengeId)})"
    }

    object Serializer : ObjectSerializer<SignInResult>("net.chronoflow.core.model.api.SignInResult") {
        override fun decode(
            json: Json,
            obj: JsonObject,
        ): SignInResult =
            when (obj.stringAt("kind")) {
                "session" -> {
                    Session(json.decodeFromJsonElement(SessionResult.serializer(), obj))
                }

                "two-factor" -> {
                    val id = obj.stringAt("challengeId")
                    if (id.isEmpty()) throw SerializationException("challengeId must not be empty")
                    TwoFactor(id)
                }

                else -> {
                    throw SerializationException("unknown sign-in kind")
                }
            }

        override fun encode(
            json: Json,
            value: SignInResult,
        ): JsonObject =
            when (value) {
                is Session -> {
                    json.encodeToJsonElement(SessionResult.serializer(), value.result) as JsonObject
                }

                is TwoFactor -> {
                    JsonObject(
                        mapOf(
                            "kind" to JsonPrimitive("two-factor"),
                            "challengeId" to JsonPrimitive(value.challengeId),
                        ),
                    )
                }
            }
    }
}

@Serializable(with = TwoFactorInput.Serializer::class)
data class TwoFactorInput(
    val challengeId: String,
    val proof: Proof,
) {
    sealed interface Proof {
        data class Code(
            val value: String,
        ) : Proof {
            override fun toString(): String = "Code(value=${redacted(value)})"
        }

        data class RecoveryCode(
            val value: String,
        ) : Proof {
            override fun toString(): String = "RecoveryCode(value=${redacted(value)})"
        }
    }

    override fun toString(): String = "TwoFactorInput(challengeId=${redacted(challengeId)}, proof=$proof)"

    object Serializer : ObjectSerializer<TwoFactorInput>("net.chronoflow.core.model.api.TwoFactorInput") {
        override fun decode(
            json: Json,
            obj: JsonObject,
        ): TwoFactorInput {
            val code = obj.optionalStringAt("code")
            val recovery = obj.optionalStringAt("recovery_code")
            val proof =
                when {
                    code != null && recovery == null -> Proof.Code(code)
                    code == null && recovery != null -> Proof.RecoveryCode(recovery)
                    else -> throw SerializationException("exactly one of code and recovery_code is required")
                }
            return TwoFactorInput(obj.stringAt("challengeId"), proof)
        }

        override fun encode(
            json: Json,
            value: TwoFactorInput,
        ): JsonObject {
            val proof =
                when (val p = value.proof) {
                    is Proof.Code -> "code" to JsonPrimitive(p.value)
                    is Proof.RecoveryCode -> "recovery_code" to JsonPrimitive(p.value)
                }
            return JsonObject(mapOf("challengeId" to JsonPrimitive(value.challengeId), proof))
        }
    }
}
