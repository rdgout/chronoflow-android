package net.chronoflow.feature.trip.state

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.icon.LucideGlyph
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.feature.trip.list.tripOutline

@Composable
fun TripPageStateCard(
    icon: LucideGlyph,
    iconTint: Color,
    title: String,
    titleStyle: TextStyle,
    text: String,
    titleTag: String,
    modifier: Modifier = Modifier,
    actions: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .tripOutline(CfTheme.colors.border, PlanMetrics.cardRadius.dp, true)
            .padding(PlanMetrics.cardPadding.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PlanMetrics.sectionGap.dp),
    ) {
        Box(
            Modifier.size(PlanMetrics.touch.dp).background(CfTheme.colors.muted, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon.imageVector(), null, tint = iconTint)
        }
        Text(
            title,
            Modifier.testTag(titleTag).semantics {
                heading()
            },
            style = titleStyle,
            color = CfTheme.colors.foreground,
            textAlign = TextAlign.Center,
        )
        Text(
            text,
            Modifier.fillMaxWidth(),
            style = CfTheme.type.authBody,
            color = CfTheme.colors.mutedForeground,
            textAlign = TextAlign.Center,
        )
        actions()
    }
}
