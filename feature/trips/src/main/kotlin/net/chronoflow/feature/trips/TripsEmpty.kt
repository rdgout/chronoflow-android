package net.chronoflow.feature.trips

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfButton
import net.chronoflow.core.designsystem.component.CfButtonSize
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.ui.R

@Composable
internal fun TripsEmpty(
    onNewTrip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CfTheme.colors
    Column(
        modifier
            .padding(top = PlanUITokens.space6.dp)
            .testTag("trips.empty")
            .drawWithCache {
                val width = AuthTokens.focusWidth.dp.toPx()
                val dash = PlanUITokens.space1.dp.toPx()
                val stroke = Stroke(width, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)))
                val inset = width / 2
                val radius = CornerRadius(PlanUITokens.radius3Xl.dp.toPx() - inset)
                val borderSize = Size(size.width - width, size.height - width)
                onDrawBehind { drawRoundRect(colors.border, Offset(inset, inset), borderSize, radius, style = stroke) }
            }.padding(horizontal = PlanUITokens.space5.dp, vertical = PlanUITokens.space8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp),
    ) {
        Box(
            Modifier.size(PlanUITokens.space14.dp).background(colors.muted, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                LucideGlyphs.Plane.imageVector(),
                null,
                Modifier.size(PlanUITokens.space6.dp),
                tint = colors.foreground,
            )
        }
        Text(
            stringResource(R.string.screens_no_trips_yet),
            Modifier.semantics { heading() },
            color = colors.foreground,
            style = CfTheme.type.style(CfTypeScale.screenCardTitle),
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.screens_your_upcoming_active_and_past_tri_893d7a),
            color = colors.mutedForeground,
            style = CfTheme.type.style(CfTypeScale.screenBodyLg),
            textAlign = TextAlign.Center,
        )
        CfButton(
            stringResource(R.string.screens_plan_a_trip),
            onNewTrip,
            Modifier.testTag("trips.create-empty"),
            size = CfButtonSize.Lg,
            fillWidth = true,
        )
    }
}
