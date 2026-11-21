package net.chronoflow.feature.settings.plan

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.contracts.int

class SettingsPlanState(
    credits: JsonObject?,
    billing: JsonObject?,
    val loaded: Boolean,
    val offline: Boolean,
) {
    val unlimited = credits?.get("unlimited") == JsonPrimitive(true)
    val credits: Int? = credits?.get("credits").int
    val subscription = billing?.get("subscription") as? JsonObject ?: JsonObject(emptyMap())
    val paymentPending = (billing?.get("checkout") as? JsonObject)?.get("status") == JsonPrimitive("pending")
    val hasBalance get() = unlimited || credits != null
    val cancelled get() = subscription["canResume"] == JsonPrimitive(true)
    val paymentFailed get() = subscription["paymentFailed"] == JsonPrimitive(true)
    val date: String? get() =
        (subscription[if (cancelled) "endsAt" else "renewsAt"] as? JsonPrimitive)
            ?.takeIf { it.isString }
            ?.content
    val interval get() = if (subscription["tier"] == JsonPrimitive("annual")) "yearly" else "monthly"
}
