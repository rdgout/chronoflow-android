package net.chronoflow.core.ui.trip

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.layout.BoardMetrics
import net.chronoflow.core.designsystem.semantics.ellipsizable
import net.chronoflow.core.designsystem.theme.CfTheme

@BoardMetrics("Trips")
private object TripChipMetrics {
    const val VERTICAL_PADDING = 2
}

@Composable
fun MetaChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    val color = LocalContentColor.current
    Text(
        text,
        modifier
            .ellipsizable()
            .border(AuthTokens.borderWidth.dp, color.copy(alpha = 0.2f), CircleShape)
            .padding(horizontal = PlanUITokens.space2.dp, vertical = TripChipMetrics.VERTICAL_PADDING.dp),
        color = color,
        style = CfTheme.type.style(CfTypeScale.screenMeta).copy(fontFeatureSettings = "tnum"),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
