package net.chronoflow.feature.trip.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.chronoflow.core.designsystem.semantics.ellipsizable
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.plan.PreparedDay
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.ui.R
import net.chronoflow.feature.trip.timeline.LocalTimelineProbe
import net.chronoflow.feature.trip.timeline.TimelineProbe.Kind
import net.chronoflow.feature.trip.wide.TripPageFrame

@Composable
fun CompactHeader(
    day: PreparedDay?,
    alpha: () -> Float,
    visible: Boolean,
    horizontalPadding: PaddingValues,
    modifier: Modifier = Modifier,
    timeline: Boolean = true,
    wide: TripPageFrame? = null,
) {
    val probe = LocalTimelineProbe.current
    SideEffect { probe?.composed(Kind.COMPACT, day?.id ?: "") }
    val thresholdDp = with(LocalDensity.current) { PlanMetrics.compactHeaderHeight.sp.toDp() }
    val bandHeight =
        with(LocalDensity.current) {
            maxOf(
                PlanMetrics.compactHeaderHeight.dp * fontScale,
                (PlanMetrics.planListTitleLineHeight + PlanMetrics.planLaneLineHeight).dp * fontScale +
                    PlanMetrics.smallGap.dp,
            )
        }
    Box(
        modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha() }
            .background(CfTheme.colors.card)
            .padding(horizontalPadding)
            .then(if (wide != null) Modifier.height(bandHeight) else Modifier.heightIn(min = thresholdDp))
            .testTag("trip.compactHeader")
            .then(if (!visible) Modifier.clearAndSetSemantics {} else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (wide != null) {
            Column(verticalArrangement = Arrangement.spacedBy(PlanMetrics.smallGap.dp)) {
                if (day != null) CompactDayTitle(day, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(PlanMetrics.wideColumnGap.dp)) {
                    LaneHeadings(null, Modifier.width(wide.timelineWidthDp.dp))
                    Spacer(Modifier.width(checkNotNull(wide.listWidthDp).dp))
                }
            }
        } else if (timeline) {
            LaneHeadings(day)
        } else if (day != null) {
            CompactDayTitle(day)
        }
    }
}

@Composable
private fun CompactDayTitle(
    day: PreparedDay,
    singleLine: Boolean = false,
) {
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
            Modifier
                .then(
                    if (singleLine) Modifier.weight(1f).ellipsizable() else Modifier,
                ).testTag("trip.compactHeader.day"),
            style = CfTheme.type.planListLine,
            color = CfTheme.colors.mutedForeground,
            maxLines = if (singleLine) 1 else Int.MAX_VALUE,
            overflow = if (singleLine) TextOverflow.Ellipsis else TextOverflow.Clip,
        )
        Spacer(
            if (singleLine) {
                Modifier.width(
                    PlanMetrics.smallGap.dp,
                )
            } else {
                Modifier.weight(1f).widthIn(min = PlanMetrics.smallGap.dp)
            },
        )
        Text(
            day.zone,
            Modifier.testTag("trip.compactHeader.zone"),
            style = CfTheme.type.planSubtitle,
            color = CfTheme.colors.mutedForeground,
        )
    }
}
