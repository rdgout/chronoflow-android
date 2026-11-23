package net.chronoflow.feature.trip.page

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.plan.PreparedPlan
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.plan.trippage.ClockLabel
import net.chronoflow.core.ui.R
import net.chronoflow.feature.trip.timeline.LocalTimelineProbe
import net.chronoflow.feature.trip.timeline.TimelineProbe.Kind

@Composable
fun SummaryCard(
    prepared: PreparedPlan,
    clocks: SummaryClockState,
    onLegend: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.plan_plan_summary)
    val colors = CfTheme.colors
    PlanCard(modifier.semantics { contentDescription = description }.testTag("trip.summary")) {
        Column(verticalArrangement = Arrangement.spacedBy(PlanMetrics.sectionGap.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PlanMetrics.smallGap.dp)) {
                    Text(stringResource(R.string.plan_your_plan), style = CfTheme.type.eyebrow, color = colors.dawnText)
                    prepared.shift?.let { Text(it, style = CfTheme.type.welcomeWordmark, color = colors.foreground) }
                }
                LegendButton(onLegend)
            }
            Text(prepared.summary, style = CfTheme.type.planListLine, color = colors.mutedForeground)
            DualClocks(clocks)
        }
    }
}

@Composable
private fun LegendButton(onLegend: (() -> Unit)?) {
    val description = stringResource(R.string.plan_legend_what_the_colours_and_patterns_mean)
    Box(
        Modifier
            .minimumInteractiveComponentSize()
            .size(PlanMetrics.touch.dp)
            .alpha(if (onLegend != null) 1f else AuthTokens.disabledOpacity)
            .background(CfTheme.colors.surface2, CircleShape)
            .clickable(enabled = onLegend != null, role = Role.Button) { onLegend?.invoke() }
            .semantics { contentDescription = description }
            .testTag("trip.legend"),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            LucideGlyphs.Info.imageVector(),
            null,
            Modifier.size(TripPageMetrics.legendIcon),
            tint = CfTheme.colors.foreground,
        )
    }
}

@Composable
fun DualClocks(
    clocks: SummaryClockState,
    modifier: Modifier = Modifier,
) {
    val probe = LocalTimelineProbe.current
    SideEffect { probe?.composed(Kind.SUMMARY_CLOCK, "summary") }
    val colors = CfTheme.colors
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(PlanMetrics.rowGap.dp)) {
        clocks.values.forEachIndexed { index, value ->
            val key =
                when (value.label) {
                    ClockLabel.Home -> R.string.plan_home_x
                    ClockLabel.There -> R.string.plan_there_x
                    ClockLabel.YoureHere -> R.string.plan_youre_here_x
                    ClockLabel.HomeTime -> R.string.plan_home_time_x
                    ClockLabel.DestinationTime -> R.string.plan_destination_time_x
                }
            val shape = RoundedCornerShape(PlanMetrics.rowRadius.dp)
            Column(
                Modifier
                    .weight(1f)
                    .background(if (value.active) colors.card else colors.surface2, shape)
                    .border(PlanMetrics.emphasisBorder.dp, if (value.active) colors.dawn else colors.surface2, shape)
                    .padding(PlanMetrics.sectionGap.dp)
                    .testTag("trip.summary.clock.$index"),
                verticalArrangement = Arrangement.spacedBy(PlanMetrics.smallGap.dp),
            ) {
                Text(
                    stringResource(key, value.place),
                    style = CfTheme.type.planSubtitle,
                    color = if (value.active) colors.dawnText else colors.mutedForeground,
                )
                Text(
                    value.time,
                    style = CfTheme.type.planTitle.copy(fontFeatureSettings = "tnum"),
                    color = colors.foreground,
                )
            }
        }
    }
}
