package net.chronoflow.core.plan.trippage

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.TimeFormat

data class PlanPreferences(
    val timeFormat: TimeFormat = TimeFormat.Value24,
    val hiddenCategories: List<BlockType> = emptyList(),
)

fun planPreferences(section: JsonObject?): PlanPreferences {
    val time = (section?.get("timeFormat") as? JsonPrimitive)?.takeIf { it.isString }?.content
    val hidden =
        (section?.get("hiddenCategories") as? JsonArray).orEmpty().mapNotNull {
            (it as? JsonPrimitive)?.takeIf { value -> value.isString }?.content?.let(BlockType::of)
        }
    return PlanPreferences(time?.let(TimeFormat::of) ?: TimeFormat.Value24, hidden)
}
