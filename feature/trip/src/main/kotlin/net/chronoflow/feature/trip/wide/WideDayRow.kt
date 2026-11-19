package net.chronoflow.feature.trip.wide

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import net.chronoflow.core.plan.generated.PlanMetrics

@Composable
fun WideDayRow(
    dayId: String,
    frame: TripPageFrame,
    timeline: @Composable () -> Unit,
    list: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Layout(
        content = {
            Box(if (frame.layout.isWide) Modifier.testTag("trip.wide.timeline.$dayId") else Modifier) { timeline() }
            if (list != null) Box { list() }
        },
        modifier = modifier.testTag("trip.day.$dayId"),
    ) { children, constraints ->
        val wide = frame.layout.isWide && children.size > 1
        val listWidth = if (wide) checkNotNull(frame.listWidthDp).dp.roundToPx() else 0
        val gap = if (wide) PlanMetrics.wideColumnGap.dp.roundToPx() else 0
        val timelineWidth = (constraints.maxWidth - listWidth - gap).coerceAtLeast(0)
        val drawing = children[0].measure(Constraints.fixedWidth(timelineWidth))
        val column = if (wide) children[1].measure(Constraints.fixed(listWidth, drawing.height)) else null
        layout(constraints.maxWidth, drawing.height) {
            drawing.placeRelative(0, 0)
            column?.placeRelative(timelineWidth + gap, 0)
        }
    }
}
