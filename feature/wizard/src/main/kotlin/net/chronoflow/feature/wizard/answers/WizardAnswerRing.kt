package net.chronoflow.feature.wizard.answers

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.PlanUITokens

internal fun Modifier.answerRing(color: Color): Modifier =
    drawWithCache {
        val width = AuthTokens.focusWidth.dp.toPx()
        val stroke = Stroke(width)
        val corner = CornerRadius(PlanUITokens.wizardCardRadius.dp.toPx() + width / 2)
        val origin = Offset(-width / 2, -width / 2)
        val area = Size(size.width + width, size.height + width)
        onDrawBehind { drawRoundRect(color, origin, area, corner, style = stroke) }
    }
