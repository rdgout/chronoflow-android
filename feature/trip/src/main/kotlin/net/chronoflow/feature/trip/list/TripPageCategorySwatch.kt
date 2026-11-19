package net.chronoflow.feature.trip.list

import android.graphics.Paint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.generated.CfDarkColors
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.plan.drawing.TimelineCategoryStyle
import net.chronoflow.core.plan.drawing.TimelinePattern
import net.chronoflow.core.plan.drawing.color
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.plan.generated.TimelinePalette
import net.chronoflow.feature.trip.timeline.TimelineGlyphPaths
import net.chronoflow.feature.trip.timeline.TimelineResources

@Composable
fun TripPageCategorySwatch(
    type: BlockType,
    modifier: Modifier = Modifier,
) {
    val colors = CfTheme.colors
    val palette = if (colors === CfDarkColors) TimelinePalette.Dark else TimelinePalette.Light
    val style = TimelineCategoryStyle(type)
    val fill = Color(palette.color(style.fill))
    val foreground = Color(palette.color(style.foreground))
    val border =
        when (type) {
            BlockType.NoCaffeine -> colors.catCaffeine
            BlockType.LightOK -> colors.catModerateFg.copy(alpha = PlanMetrics.tagOpacity.toFloat())
            BlockType.Note -> colors.border
            else -> null
        }
    Box(
        modifier
            .size(PlanMetrics.prioritySampleHeight.dp)
            .clip(RoundedCornerShape(PlanMetrics.tileRadius.dp))
            .clearAndSetSemantics {}
            .drawWithCache {
                val corner = CornerRadius(PlanMetrics.tileRadius.dp.toPx())
                val pattern =
                    style.pattern?.let {
                        val dots = it == TimelinePattern.DOTS
                        val opacity = if (dots) PlanMetrics.dotOpacity else PlanMetrics.hatchOpacity
                        Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = foreground.copy(alpha = opacity.toFloat()).toArgb()
                            shader =
                                if (dots) {
                                    TimelineResources.dotShader(density)
                                } else {
                                    TimelineResources.hatchShader(density, color)
                                }
                        }
                    }
                val borderDp =
                    if (type == BlockType.NoCaffeine) {
                        PlanMetrics.emphasisBorder
                    } else {
                        PlanMetrics.border
                    }
                val width = borderDp.dp.toPx()
                val inset = width / 2
                val borderOffset = Offset(inset, inset)
                val borderSize = Size(size.width - width, size.height - width)
                val borderCorner = CornerRadius(corner.x - inset)
                val dash = PlanMetrics.smallGap.dp.toPx()
                val effect = if (style.dashed) PathEffect.dashPathEffect(floatArrayOf(dash, dash)) else null
                val stroke = Stroke(width, pathEffect = effect)
                val slashInset = PlanMetrics.tilePadding.dp.toPx()
                val slashFar = PlanMetrics.prioritySampleHeight.dp.toPx() - slashInset
                val slashStart = Offset(slashInset, slashFar)
                val slashEnd = Offset(slashStart.y, slashStart.x)
                val slashWidth = PlanMetrics.emphasisBorder.dp.toPx()
                onDrawWithContent {
                    drawRoundRect(fill, cornerRadius = corner)
                    if (pattern != null) {
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, pattern)
                        }
                    }
                    drawContent()
                    if (type == BlockType.NoCaffeine) drawLine(foreground, slashStart, slashEnd, slashWidth)
                    if (border != null) drawRoundRect(border, borderOffset, borderSize, borderCorner, style = stroke)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        style.glyph?.let { glyph ->
            Icon(
                TimelineGlyphPaths.source(glyph).imageVector(),
                null,
                Modifier.size(PlanMetrics.blockIcon.dp),
                tint = foreground,
            )
        }
    }
}
