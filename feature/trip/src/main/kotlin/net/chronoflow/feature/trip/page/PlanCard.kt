package net.chronoflow.feature.trip.page

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.plan.generated.PlanMetrics

@Composable
fun PlanCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(PlanMetrics.cardRadius.dp)
    Column(
        modifier
            .fillMaxWidth()
            .background(CfTheme.colors.card, shape)
            .border(PlanMetrics.border.dp, CfTheme.colors.border, shape)
            .padding(PlanMetrics.cardPadding.dp),
        content = content,
    )
}
