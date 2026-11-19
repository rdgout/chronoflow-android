package net.chronoflow.feature.trip.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.plan.PreparedDay
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.ui.R
import net.chronoflow.feature.trip.timeline.LocalTimelineProbe
import net.chronoflow.feature.trip.timeline.TimelineProbe.Kind

@Composable
fun CompactHeader(
    day: PreparedDay?,
    alpha: () -> Float,
    visible: Boolean,
    horizontalPadding: PaddingValues,
    modifier: Modifier = Modifier,
    timeline: Boolean = true,
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
    ) {
        if (timeline) {
            LaneHeadings(day)
        } else if (day != null) {
            val label = stringResource(R.string.plan_day_title_date, day.title, day.date)
            val titleStyle =
                CfTheme.type.planListTitle
                    .toSpanStyle()
                    .copy(color = CfTheme.colors.foreground)
            val text =
                remember(label, titleStyle) {
                    AnnotatedString(label, spanStyles = listOf(AnnotatedString.Range(titleStyle, 0, day.title.length)))
                }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text,
                    Modifier.testTag("trip.compactHeader.day"),
                    style = CfTheme.type.planListLine,
                    color = CfTheme.colors.mutedForeground,
                )
                Spacer(Modifier.weight(1f).widthIn(min = PlanMetrics.smallGap.dp))
                Text(
                    day.zone,
                    Modifier.testTag("trip.compactHeader.zone"),
                    style = CfTheme.type.planSubtitle,
                    color = CfTheme.colors.mutedForeground,
                )
            }
        }
    }
}
