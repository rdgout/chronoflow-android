package net.chronoflow.core.model.session

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.chronoflow.core.model.api.ApiJson

data class StoredSession(
    val token: String,
    val accountId: String,
) {
    override fun toString(): String = "StoredSession(token=<redacted>, accountId=$accountId)"

    fun encode(): ByteArray =
        ApiJson.response
            .encodeToString(
                JsonObject.serializer(),
                buildJsonObject {
                    put("token", token)
                    put("accountId", accountId)
                },
            ).encodeToByteArray()

    companion object {
        fun parse(bytes: ByteArray): StoredSession? {
            val obj =
                try {
                    ApiJson.response.parseToJsonElement(bytes.decodeToString()) as? JsonObject
                } catch (_: SerializationException) {
                    null
                } catch (_: IllegalArgumentException) {
                    null
                } ?: return null

            fun string(key: String): String? =
                (obj[key] as? JsonPrimitive)
                    ?.takeIf { it.isString && it.content.trim().isNotEmpty() }
                    ?.content
            return StoredSession(string("token") ?: return null, string("accountId") ?: return null)
        }
    }
}
