package net.chronoflow.core.ui.plan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.model.contracts.Priority
import net.chronoflow.core.plan.generated.PlanMetrics

@Composable
fun PrioritySample(
    priority: Priority,
    modifier: Modifier = Modifier,
) {
    PriorityDrawing(priority, false, modifier)
}

@Composable
internal fun PriorityDrawing(
    priority: Priority,
    keyStyle: Boolean,
    modifier: Modifier = Modifier,
) {
    val c = CfTheme.colors
    val radius = if (keyStyle) PlanMetrics.tileRadius.toFloat() else PlanUITokens.radiusLg
    Box(
        modifier
            .size(PlanUITokens.prioritySampleWidth.dp, PlanUITokens.prioritySampleHeight.dp)
            .clearAndSetSemantics {}
            .clip(RoundedCornerShape(radius.dp))
            .drawWithCache {
                val border =
                    (
                        if (keyStyle &&
                            priority == Priority.Core
                        ) {
                            PlanMetrics.coreBorder.toFloat()
                        } else {
                            AuthTokens.borderWidth
                        }
                    ).dp.toPx()
                val dash = PlanUITokens.space1.dp.toPx()
                val stroke =
                    Stroke(
                        border,
                        pathEffect =
                            if (priority ==
                                Priority.Optional
                            ) {
                                PathEffect.dashPathEffect(floatArrayOf(dash, dash))
                            } else {
                                null
                            },
                    )
                val paint =
                    Paint().apply {
                        color = if (priority == Priority.Info) c.muted else c.catSleep
                        if (priority ==
                            Priority.Info
                        ) {
                            colorFilter =
                                ColorFilter.colorMatrix(
                                    ColorMatrix().apply { setToSaturation(PlanUITokens.priorityInfoSaturation) },
                                )
                        }
                    }
                val corner = radius.dp.toPx()
                val ink =
                    if (keyStyle) {
                        c.foreground
                    } else if (priority ==
                        Priority.Optional
                    ) {
                        c.foreground.copy(alpha = PlanUITokens.priorityOptionalOpacity)
                    } else {
                        c.border
                    }
                val bar = PlanUITokens.space1.dp.toPx()
                val barColor = c.foreground.copy(alpha = PlanUITokens.priorityCoreOpacity)
                val inset = Offset(border / 2, border / 2)
                val inner = Size(size.width - border, size.height - border)
                val innerCorner = CornerRadius(corner - border / 2)
                onDrawBehind {
                    if (!keyStyle) {
                        drawIntoCanvas {
                            it.drawRoundRect(
                                0f,
                                0f,
                                size.width,
                                size.height,
                                corner,
                                corner,
                                paint,
                            )
                        }
                    }
                    drawRoundRect(ink, inset, inner, innerCorner, style = stroke)
                    if (!keyStyle && priority == Priority.Core) drawRect(barColor, size = Size(bar, size.height))
                }
            },
    )
}
