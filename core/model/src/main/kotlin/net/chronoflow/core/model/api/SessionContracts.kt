package net.chronoflow.core.model.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.contracts.ObjectSerializer
import net.chronoflow.core.model.contracts.optionalStringAt
import net.chronoflow.core.model.contracts.require
import net.chronoflow.core.model.contracts.stringAt

typealias AccountIdentity = SessionAccount

@Serializable
data class AccountPolicies(
    val termsVersion: String,
    val privacyVersion: String,
    val acceptedTermsVersion: String? = null,
    val acceptedTermsAt: String? = null,
)

@Serializable
data class SessionAccount(
    val id: String,
    val name: String,
    val email: String,
    val hasVerifiedEmail: Boolean,
    val policies: AccountPolicies,
    val walkthroughSeen: Boolean,
)

@Serializable(with = SessionResult.Serializer::class)
data class SessionResult(
    val token: String,
    val account: SessionAccount,
    val tripId: String? = null,
) {
    override fun toString(): String = "SessionResult(token=${redacted(token)}, account=$account, tripId=$tripId)"

    object Serializer : ObjectSerializer<SessionResult>("net.chronoflow.core.model.api.SessionResult") {
        override fun decode(
            json: Json,
            obj: JsonObject,
        ): SessionResult {
            if (obj.stringAt("kind") != "session") throw SerializationException("kind must be session")
            val token = obj.stringAt("token")
            val account = json.decodeFromJsonElement(SessionAccount.serializer(), obj.require("account"))
            val tripId = obj.optionalStringAt("tripId")
            if (token.isBlank()) throw SerializationException("token must not be blank")
            try {
                AccountID.of(account.id)
            } catch (e: AccountID.InvalidUUID) {
                throw SerializationException("account id must be a UUID", e)
            }
            return SessionResult(token, account, tripId)
        }

        override fun encode(
            json: Json,
            value: SessionResult,
        ): JsonObject =
            JsonObject(
                mapOf(
                    "kind" to JsonPrimitive("session"),
                    "token" to JsonPrimitive(value.token),
                    "account" to json.encodeToJsonElement(SessionAccount.serializer(), value.account),
                    "tripId" to (value.tripId?.let { JsonPrimitive(it) } ?: JsonNull),
                ),
            )
    }
}
