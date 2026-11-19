package net.chronoflow.feature.trip.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.plan.generated.PlanMetrics

@Composable
fun TripPagePriorityTag(
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = CfTheme.colors
    val fill = colors.background.copy(alpha = PlanMetrics.tagOpacity.toFloat())
    val border = colors.foreground.copy(alpha = PlanMetrics.tagOpacity.toFloat())
    Text(
        label,
        modifier
            .background(fill, CircleShape)
            .border(PlanMetrics.border.dp, border, CircleShape)
            .padding(horizontal = PlanMetrics.tilePadding.dp, vertical = PlanMetrics.border.dp),
        style = CfTheme.type.planPriority,
        color = colors.cardForeground,
    )
}
