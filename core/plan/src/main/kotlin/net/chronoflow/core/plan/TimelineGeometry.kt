package net.chronoflow.core.plan

import net.chronoflow.core.plan.generated.PlanMetrics

/** CGRect semantics: `contains` is half-open on the max edges. */
data class TimelineRect(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
) {
    val minX: Double get() = x
    val maxX: Double get() = x + width
    val minY: Double get() = y
    val maxY: Double get() = y + height
    val midX: Double get() = x + width / 2
    val midY: Double get() = y + height / 2

    fun contains(
        px: Double,
        py: Double,
    ): Boolean = px >= minX && px < maxX && py >= minY && py < maxY
}

data class TimelineTileFrame(
    val blockID: String,
    val frame: TimelineRect,
    val hitFrame: TimelineRect,
    val tiny: Boolean,
    val continuesBefore: Boolean,
    val continuesAfter: Boolean,
)

data class TimelineGeometry(
    val width: Double,
    val height: Double,
    val laneWidth: Double,
    val tiles: List<TimelineTileFrame>,
) {
    companion object {
        operator fun invoke(
            layout: DayLayout,
            availableWidth: Double,
        ): TimelineGeometry {
            val width = maxOf(layout.minWidth, availableWidth)
            val height = layout.height
            val laneWidth = (width - PlanMetrics.gutterWidth) / timelineLanes.size - PlanMetrics.laneGap
            val tiles = ArrayList<TimelineTileFrame>()
            for ((index, lane) in timelineLanes.withIndex()) {
                for (placed in layout.placed[lane].orEmpty()) {
                    val column = laneWidth / placed.cols
                    val x =
                        PlanMetrics.gutterWidth + PlanMetrics.laneGap + index * (laneWidth + PlanMetrics.laneGap) +
                            placed.col * column + PlanMetrics.tileInset
                    val frame =
                        TimelineRect(x, placed.top, maxOf(0.0, column - PlanMetrics.tileInset * 2), placed.height)
                    // Android raises the iOS 44 pt hit target to 48 dp (04 OQ-T5, plan A10).
                    val targetWidth = maxOf(PlanMetrics.minTouchTarget, frame.width)
                    val targetHeight = maxOf(PlanMetrics.minTouchTarget, frame.height)
                    tiles.add(
                        TimelineTileFrame(
                            blockID = placed.block.id,
                            frame = frame,
                            hitFrame =
                                TimelineRect(
                                    x = maxOf(0.0, minOf(x - (targetWidth - frame.width) / 2, width - targetWidth)),
                                    y =
                                        maxOf(
                                            0.0,
                                            minOf(
                                                placed.top - (targetHeight - frame.height) / 2,
                                                height - targetHeight,
                                            ),
                                        ),
                                    width = targetWidth,
                                    height = targetHeight,
                                ),
                            tiny = placed.tiny,
                            continuesBefore = placed.continuesBefore,
                            continuesAfter = placed.continuesAfter,
                        ),
                    )
                }
            }
            return TimelineGeometry(width, height, laneWidth, tiles)
        }
    }
}
