package net.chronoflow.core.plan.drawing

import net.chronoflow.core.model.contracts.Language

/** Everything that changes a day's drawing; `density` and `compact` extend the plan's key as iOS `RenderKey` does. */
data class DayDrawingKey(
    val dayId: String,
    val revision: String,
    val availableWidthPx: Int,
    val density: Float,
    val textScale: Float,
    val dark: Boolean,
    val compact: Boolean,
    val language: Language,
)
