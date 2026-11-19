package net.chronoflow.feature.trip.wide

import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.plan.trippage.TripPageLayout

data class TripPageFrame(
    val layout: TripPageLayout,
    val gutterDp: Double,
    val compact: Boolean,
    val listWidthDp: Double?,
    val timelineWidthDp: Double,
) {
    companion object {
        fun resolve(
            windowWidthDp: Double,
            contentWidthDp: Double,
        ): TripPageFrame {
            val layout = TripPageLayout.resolve(windowWidthDp)
            val columns = layout.listWidth?.let { it + PlanMetrics.wideColumnGap } ?: 0.0
            return TripPageFrame(
                layout,
                layout.gutter,
                windowWidthDp < PlanMetrics.timelineLabelBreakpoint,
                layout.listWidth,
                (contentWidthDp - 2 * layout.gutter - columns).coerceAtLeast(0.0),
            )
        }
    }
}
