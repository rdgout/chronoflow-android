package net.chronoflow.core.plan

import net.chronoflow.core.plan.generated.PlanMetrics

fun timelineBlockFrame(
    blockID: String,
    geometry: TimelineGeometry,
    layout: DayLayout,
): TimelineRect? {
    val tile = geometry.tiles.firstOrNull { it.blockID == blockID }
    if (tile != null) return tile.hitFrame
    val target = PlanMetrics.minTouchTarget

    fun clampY(top: Double): Double = maxOf(0.0, minOf(top - target / 2, geometry.height - target))
    val marker = layout.markers.firstOrNull { it.block.id == blockID }
    if (marker != null) return TimelineRect(0.0, clampY(marker.top), maxOf(PlanMetrics.gutterWidth, target), target)
    val change = layout.changes.firstOrNull { it.block.id == blockID }
    if (change != null) {
        val x = PlanMetrics.gutterWidth + PlanMetrics.laneGap
        return TimelineRect(x, clampY(change.top), geometry.width - x, target)
    }
    return null
}
