package net.chronoflow.feature.trip.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.semantics.ellipsizable
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.plan.PreparedDay
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.ui.R

@Composable
fun LaneHeadings(
    day: PreparedDay?,
    modifier: Modifier = Modifier,
) {
    val description = day?.let { stringResource(R.string.plan_title_comma_date, it.title, it.date) }
    val colors = CfTheme.colors
    Row(
        modifier.fillMaxWidth().testTag("trip.lanes").clearAndSetSemantics {
            if (description !=
                null
            ) {
                contentDescription = description
            }
        },
        horizontalArrangement = Arrangement.spacedBy(PlanMetrics.laneGap.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(PlanMetrics.gutterWidth.dp), contentAlignment = Alignment.CenterEnd) {
            if (day !=
                null
            ) {
                Column(Modifier.padding(end = PlanMetrics.smallGap.dp), horizontalAlignment = Alignment.End) {
                    Text(day.weekday, style = CfTheme.type.planCompactWeekday, color = colors.foreground)
                    Text(
                        day.dayMonth,
                        style = CfTheme.type.planCompactDate.copy(fontFeatureSettings = "tnum"),
                        color = colors.mutedForeground,
                    )
                }
            }
        }
        val labels = listOf(R.string.plan_sleep, R.string.plan_light, R.string.plan_caffeine, R.string.plan_travel)
        val laneColors = listOf(colors.catSleep, colors.catLight, colors.catCaffeine, colors.catTravel)
        labels.forEachIndexed { index, label ->
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(PlanMetrics.smallGap.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(PlanMetrics.dot.dp).background(laneColors[index], CircleShape))
                Text(
                    stringResource(label),
                    Modifier.ellipsizable(),
                    style = CfTheme.type.planLane,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = colors.mutedForeground,
                )
            }
        }
    }
}
