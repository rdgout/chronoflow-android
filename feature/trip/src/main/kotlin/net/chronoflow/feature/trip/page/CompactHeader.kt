package net.chronoflow.feature.trip.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.sp
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.plan.PreparedDay
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.feature.trip.timeline.LocalTimelineProbe
import net.chronoflow.feature.trip.timeline.TimelineProbe.Kind

@Composable
fun CompactHeader(
    day: PreparedDay?,
    alpha: () -> Float,
    visible: Boolean,
    horizontalPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val probe = LocalTimelineProbe.current
    SideEffect { probe?.composed(Kind.COMPACT, day?.id ?: "") }
    val thresholdDp = with(LocalDensity.current) { PlanMetrics.compactHeaderHeight.sp.toDp() }
    Box(
        modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha() }
            .background(CfTheme.colors.card)
            .padding(horizontalPadding)
            .heightIn(min = thresholdDp)
            .testTag("trip.compactHeader")
            .then(if (!visible) Modifier.clearAndSetSemantics {} else Modifier),
        contentAlignment = Alignment.Center,
    ) { LaneHeadings(day) }
}
