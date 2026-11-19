package net.chronoflow.feature.today.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.effect.softShadow
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.theme.CfTheme

@Composable
internal fun Modifier.todayCard(shadow: Boolean = false): Modifier {
    val colors = CfTheme.colors
    val shape = RoundedCornerShape(PlanUITokens.radius3Xl.dp)
    return then(if (shadow) Modifier.softShadow(shape, colors.isDark) else Modifier)
        .background(colors.card, shape)
        .border(AuthTokens.borderWidth.dp, colors.border, shape)
}

@Composable
internal fun Modifier.todayDashedBorder(thick: Boolean = false): Modifier {
    val color = CfTheme.colors.border
    val width = if (thick) AuthTokens.focusWidth.dp else AuthTokens.borderWidth.dp
    return drawWithCache {
        val dash = PlanUITokens.space1.dp.toPx()
        val stroke = Stroke(width.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)))
        val inset = stroke.width / 2
        val radius = CornerRadius(PlanUITokens.radius3Xl.dp.toPx() - inset)
        val borderSize = Size(size.width - stroke.width, size.height - stroke.width)
        onDrawBehind { drawRoundRect(color, Offset(inset, inset), borderSize, radius, style = stroke) }
    }
}
