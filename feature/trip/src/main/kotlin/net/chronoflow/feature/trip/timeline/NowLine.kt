package net.chronoflow.feature.trip.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.ui.R
import kotlin.math.roundToInt

/**
 * Port of `DayTimelineView.swift:87-98` and its accessibility child (l.119-122): the only reader of a [DayClock]. It is
 * placed in layout from the minute state and has its own layer, so a tick invalidates nothing but this node.
 */
@Composable
fun NowLine(
    clock: DayClock,
    dayId: String,
    widthDp: Double,
    modifier: Modifier = Modifier,
) {
    val probe = LocalTimelineProbe.current
    SideEffect { probe?.composed(TimelineProbe.Kind.CLOCK, dayId) }
    val value = clock.value.value ?: return
    val colors = timelineColors()
    val density = LocalDensity.current
    val pillHeight = with(density) { PlanMetrics.planNowLineHeight.sp.toDp() }
    val description = stringResource(R.string.plan_now_x, value.text)
    Box(
        modifier
            .width(widthDp.dp)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val y = (value.offset.dp.toPx() - placeable.height / 2f).roundToInt()
                layout(placeable.width, placeable.height) { placeable.place(IntOffset(0, y)) }
            }.graphicsLayer()
            .clearAndSetSemantics {
                contentDescription = description
                traversalIndex = -1f
            },
    ) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .height(PlanMetrics.emphasisBorder.dp)
                .background(colors.now),
        )
        Text(
            text = value.text,
            style = TimelineStyles.planNow,
            color = colors.primaryForeground,
            modifier =
                Modifier
                    .align(Alignment.CenterStart)
                    .heightIn(min = pillHeight)
                    .background(colors.now, RoundedCornerShape(percent = 50))
                    .padding(horizontal = PlanMetrics.tilePadding.dp),
        )
    }
}
