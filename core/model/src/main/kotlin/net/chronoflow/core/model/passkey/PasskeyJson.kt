package net.chronoflow.core.model.passkey

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.api.ApiPasskeyAssertion
import net.chronoflow.core.model.api.ApiPasskeyRequestOptions

object PasskeyJson {
    fun requestJson(options: ApiPasskeyRequestOptions): String =
        ApiJson.request.encodeToString(ApiPasskeyRequestOptions.serializer(), options)

    fun assertion(responseJson: String): ApiPasskeyAssertion {
        val root =
            try {
                ApiJson.response.parseToJsonElement(responseJson) as? JsonObject ?: throw PasskeyFailure.Unavailable
            } catch (_: SerializationException) {
                throw PasskeyFailure.Unavailable
            }
        if (root.requiredString("type") != "public-key") throw PasskeyFailure.Unavailable
        val rawId = root.requiredString(if ("rawId" in root) "rawId" else "id")
        val credentialID = Base64URL.decodeLenient(rawId)
        if ("id" in root && !Base64URL.decodeLenient(root.requiredString("id")).contentEquals(credentialID)) {
            throw PasskeyFailure.InvalidEncoding
        }
        val response = root["response"] as? JsonObject ?: throw PasskeyFailure.Unavailable
        val userHandle =
            when (val value = response["userHandle"]) {
                null, JsonNull -> {
                    null
                }

                else -> {
                    val text = value as? JsonPrimitive ?: throw PasskeyFailure.Unavailable
                    if (!text.isString) throw PasskeyFailure.Unavailable
                    text.content.takeIf { it.isNotEmpty() }?.let(Base64URL::decodeLenient)
                }
            }
        return Base64URL.assertion(
            credentialID,
            Base64URL.decodeLenient(response.requiredString("clientDataJSON")),
            Base64URL.decodeLenient(response.requiredString("authenticatorData")),
            Base64URL.decodeLenient(response.requiredString("signature")),
            userHandle,
        )
    }

    private fun JsonObject.requiredString(key: String): String {
        val value = this[key] as? JsonPrimitive ?: throw PasskeyFailure.Unavailable
        if (!value.isString || value.content.isEmpty()) throw PasskeyFailure.Unavailable
        return value.content
    }
}
