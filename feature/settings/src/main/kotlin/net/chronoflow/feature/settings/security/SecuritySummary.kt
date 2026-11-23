package net.chronoflow.feature.settings.security

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.store.SettingsRecord

data class SecuritySummary(
    val twoFactorEnabled: Boolean,
    val twoFactorConfirmedAt: String?,
    val passkeyCount: Int?,
) {
    companion object {
        fun from(settings: List<SettingsRecord>): SecuritySummary? {
            val raw = settings.firstOrNull { it.section == "security" }?.value ?: return null
            val value =
                try {
                    ApiJson.response.parseToJsonElement(raw) as? JsonObject
                } catch (_: SerializationException) {
                    null
                } catch (_: IllegalArgumentException) {
                    null
                }
            return from(value)
        }

        fun from(value: JsonObject?): SecuritySummary? {
            value ?: return null
            val confirmed = (value["twoFactorConfirmedAt"] as? JsonPrimitive)?.takeIf { it.isString }?.content
            val count =
                (value["passkeyCount"] as? JsonPrimitive)
                    ?.takeUnless { it.isString }
                    ?.content
                    ?.toBigDecimalOrNull()
            val integral =
                try {
                    count?.intValueExact()?.takeIf { it >= 0 }
                } catch (_: ArithmeticException) {
                    null
                }
            return SecuritySummary(value["twoFactorEnabled"] == JsonPrimitive(true), confirmed, integral)
        }
    }
}
