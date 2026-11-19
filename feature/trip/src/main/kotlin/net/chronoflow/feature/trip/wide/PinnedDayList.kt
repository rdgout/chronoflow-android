package net.chronoflow.feature.trip.wide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.feature.trip.timeline.LocalTimelineProbe
import net.chronoflow.feature.trip.timeline.TimelineProbe

@Composable
fun PinnedDayList(
    dayId: String,
    rows: @Composable ColumnScope.() -> Unit,
    pin: WidePinSource,
    modifier: Modifier = Modifier,
) {
    val probe = LocalTimelineProbe.current
    SideEffect { probe?.composed(TimelineProbe.Kind.LIST, dayId) }
    val itemKey = remember(dayId) { "t:$dayId" }
    Layout(content = {
        Box(Modifier.testTag("trip.wide.list.$dayId").clipToBounds()) {
            Column(
                Modifier
                    .testTag("trip.wide.list.scroll.$dayId")
                    .verticalScroll(rememberScrollState())
                    .padding(top = PlanMetrics.rowGap.dp),
                verticalArrangement = Arrangement.spacedBy(PlanMetrics.rowGap.dp),
                content = rows,
            )
        }
    }, modifier = modifier) { children, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val viewport = pin.position(itemKey, height, Int.MAX_VALUE).heightPx
        // A clock child can change row height without recomposing this parent.
        val child = children[0].measure(Constraints(minWidth = width, maxWidth = width, maxHeight = viewport))
        layout(width, height) {
            child.placeRelative(0, pin.position(itemKey, height, child.height).offsetPx)
        }
    }
}
