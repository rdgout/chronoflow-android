package net.chronoflow.core.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.theme.CfTheme

@Composable
fun PriorityTag(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = CfTheme.colors
    val fill = colors.background.copy(alpha = PlanUITokens.priorityTagOpacity)
    val stroke = colors.foreground.copy(alpha = PlanUITokens.priorityTagOpacity)
    Text(
        text,
        modifier
            .background(fill, CircleShape)
            .border(AuthTokens.borderWidth.dp, stroke, CircleShape)
            .padding(
                horizontal = (PlanUITokens.space1 + PlanUITokens.space1 / 2).dp,
                vertical = AuthTokens.borderWidth.dp,
            ),
        color = colors.foreground,
        style = CfTheme.type.style(CfTypeScale.screenPriorityTag),
    )
}
