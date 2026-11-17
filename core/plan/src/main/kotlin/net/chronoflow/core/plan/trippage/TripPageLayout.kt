package net.chronoflow.core.plan.trippage

import net.chronoflow.core.plan.generated.PlanMetrics

data class TripPageLayout(
    val listWidth: Double?,
) {
    val isWide: Boolean get() = listWidth != null
    val gutter: Double get() = if (isWide) PlanMetrics.wideGutter else PlanMetrics.gutter

    companion object {
        /** Width only: an unfolded device held flat is landscape and is the widest case, not a phone. */
        fun resolve(width: Double): TripPageLayout {
            if (width < PlanMetrics.wideBreakpoint) return TripPageLayout(null)
            return TripPageLayout(
                if (width >=
                    PlanMetrics.wideLargeBreakpoint
                ) {
                    PlanMetrics.wideLargeListWidth
                } else {
                    PlanMetrics.wideListWidth
                },
            )
        }
    }
}

data class TripWideListPosition(
    val offset: Double,
    val height: Double,
) {
    companion object {
        operator fun invoke(
            dayTop: Double,
            dayHeight: Double,
            viewportHeight: Double,
            stickyTop: Double,
            contentHeight: Double,
        ): TripWideListPosition {
            val offset = minOf(maxOf(0.0, stickyTop - dayTop), maxOf(0.0, dayHeight))
            val height =
                maxOf(
                    0.0,
                    minOf(dayHeight - offset, viewportHeight - stickyTop, PlanMetrics.wideListMaxHeight, contentHeight),
                )
            return TripWideListPosition(offset, height)
        }
    }
}
