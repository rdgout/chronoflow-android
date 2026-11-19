package net.chronoflow.feature.trip.list

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.chronoflow.core.plan.generated.PlanMetrics

internal fun Modifier.tripOutline(
    color: Color,
    radius: Dp,
    dashed: Boolean,
    selected: () -> Boolean = { false },
    selectedColor: Color = color,
    selectionOnly: Boolean = false,
): Modifier =
    drawWithCache {
        val width = PlanMetrics.border.dp.toPx()
        val inset = width / 2
        val corner = CornerRadius((radius.toPx() - inset).coerceAtLeast(0f))
        val origin = Offset(inset, inset)
        val bounds = Size((size.width - width).coerceAtLeast(0f), (size.height - width).coerceAtLeast(0f))
        val dash = PlanMetrics.smallGap.dp.toPx()
        val effect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(dash, dash)) else null
        val stroke = Stroke(width, pathEffect = effect)
        val selectedWidth = PlanMetrics.emphasisBorder.dp.toPx()
        val selectedInset = selectedWidth / 2
        val selectedCorner = CornerRadius((radius.toPx() - selectedInset).coerceAtLeast(0f))
        val selectedOrigin = Offset(selectedInset, selectedInset)
        val selectedBounds =
            Size((size.width - selectedWidth).coerceAtLeast(0f), (size.height - selectedWidth).coerceAtLeast(0f))
        val selectedStroke = Stroke(selectedWidth)
        onDrawWithContent {
            drawContent()
            if (selected()) {
                drawRoundRect(selectedColor, selectedOrigin, selectedBounds, selectedCorner, style = selectedStroke)
            } else if (!selectionOnly) {
                drawRoundRect(color, origin, bounds, corner, style = stroke)
            }
        }
    }
