package net.chronoflow.feature.trip.timeline

import android.graphics.Canvas
import android.graphics.Paint
import net.chronoflow.core.plan.DayLayout
import net.chronoflow.core.plan.TimelineGeometry
import net.chronoflow.core.plan.drawing.TimelineColors
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.plan.generated.TimelinePalette
import net.chronoflow.core.plan.timelineLanes

/**
 * What a day shows until its drawing is ready: the lanes in `surface2` and the visible tick lines, with the geometry and
 * colours of the 1c builder's draw-order items 1 and 2. All arrays and paints are made once per (day, key), never in draw.
 */
internal class DayPlaceholder(
    layout: DayLayout,
    geometry: TimelineGeometry,
    density: Float,
    palette: TimelinePalette,
) {
    private val lanes = FloatArray(timelineLanes.size * 4)
    private val lines: FloatArray
    private val fill =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = palette.surface2
        }
    private val stroke =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.BUTT
            strokeWidth = (PlanMetrics.line * density).toFloat()
            color = TimelineColors.withAlpha(palette.border, PlanMetrics.gridOpacity)
        }

    init {
        val laneWidth = (geometry.laneWidth * density).toFloat()
        val height = (layout.height * density).toFloat()

        fun laneX(index: Int) =
            (
                (PlanMetrics.gutterWidth + PlanMetrics.laneGap + index * (geometry.laneWidth + PlanMetrics.laneGap)) *
                    density
            ).toFloat()
        for (index in timelineLanes.indices) {
            lanes[index * 4] = laneX(index)
            lanes[index * 4 + 1] = 0f
            lanes[index * 4 + 2] = laneX(index) + laneWidth
            lanes[index * 4 + 3] = height
        }
        val visible = layout.ticks.count { it.visible }
        lines = FloatArray(visible * timelineLanes.size * 4)
        var at = 0
        for (tick in layout.ticks) {
            if (!tick.visible) continue
            val y = (tick.top * density).toFloat()
            for (index in timelineLanes.indices) {
                lines[at++] = laneX(index)
                lines[at++] = y
                lines[at++] = laneX(index) + laneWidth
                lines[at++] = y
            }
        }
    }

    fun draw(canvas: Canvas) {
        var i = 0
        while (i < lanes.size) {
            canvas.drawRect(lanes[i], lanes[i + 1], lanes[i + 2], lanes[i + 3], fill)
            i += 4
        }
        if (lines.isNotEmpty()) canvas.drawLines(lines, stroke)
    }
}
