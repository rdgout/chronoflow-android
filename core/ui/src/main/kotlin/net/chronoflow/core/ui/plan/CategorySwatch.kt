package net.chronoflow.core.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.layout.BoardMetrics
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.plan.drawing.TimelineCategoryStyle
import net.chronoflow.core.plan.drawing.TimelineGlyph
import net.chronoflow.core.plan.drawing.TimelinePattern
import net.chronoflow.core.plan.drawing.color
import net.chronoflow.core.plan.generated.TimelinePalette

enum class CategorySwatchSize { Small, Regular }

@BoardMetrics("Trips")
private object CategorySwatchMetrics {
    const val SMALL_GLYPH = 14
}

private class SwatchStyle(
    val fill: Color,
    val foreground: Color,
    val glyph: TimelineGlyph?,
    val pattern: TimelinePattern?,
    val dashed: Boolean,
)

/** iOS swatches also dash Note; Unknown uses travel colours and the info glyph, unlike the timeline. */
private fun swatchStyle(
    type: BlockType,
    dark: Boolean,
): SwatchStyle {
    val palette = if (dark) TimelinePalette.Dark else TimelinePalette.Light
    val style = TimelineCategoryStyle(type)
    val unknown = type is BlockType.Unknown
    return SwatchStyle(
        Color(if (unknown) palette.catTravel else palette.color(style.fill)),
        Color(if (unknown) palette.catTravelFg else palette.color(style.foreground)),
        if (unknown) TimelineGlyph.INFO else style.glyph,
        style.pattern,
        style.dashed || type == BlockType.Note,
    )
}

@Composable
fun CategorySwatch(
    type: BlockType,
    modifier: Modifier = Modifier,
    size: CategorySwatchSize = CategorySwatchSize.Regular,
) {
    val dark = CfTheme.colors.isDark
    val style = remember(type, dark) { swatchStyle(type, dark) }
    val shape = RoundedCornerShape(PlanUITokens.radiusLg.dp)
    val dimension =
        if (size ==
            CategorySwatchSize.Small
        ) {
            PlanUITokens.space6 + PlanUITokens.space1
        } else {
            PlanUITokens.space8
        }
    val glyph = remember(style.glyph) { style.glyph?.let { glyph(it).imageVector() } }
    Box(
        modifier.size(dimension.dp).clearAndSetSemantics {}.clip(shape).background(style.fill).drawWithCache {
            val pattern = Path()
            if (style.pattern == TimelinePattern.DOTS) {
                val spacing = PlanUITokens.patternDotSpacing.dp.toPx()
                val diameter = PlanUITokens.patternDotDiameter.dp.toPx()
                var x = 0f
                while (x <= this.size.width) {
                    var y = 0f
                    while (y <=
                        this.size.height
                    ) {
                        pattern.addOval(Rect(Offset(x, y), Size(diameter, diameter)))
                        y += spacing
                    }
                    x += spacing
                }
            } else if (style.pattern == TimelinePattern.HATCH) {
                var x = -this.size.height
                val spacing = PlanUITokens.patternHatchSpacing.dp.toPx()
                while (x <=
                    this.size.width
                ) {
                    pattern.moveTo(x, this.size.height)
                    pattern.lineTo(x + this.size.height, 0f)
                    x +=
                        spacing
                }
            }
            val hatchStroke = Stroke(PlanUITokens.patternHatchWidth.dp.toPx())
            val dash = PlanUITokens.space1.dp.toPx()
            val width = AuthTokens.borderWidth.dp.toPx()
            val dashedStroke = Stroke(width, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)))
            val inset = width / 2
            val radius = CornerRadius(PlanUITokens.radiusLg.dp.toPx() - inset)
            val borderSize = Size(this.size.width - width, this.size.height - width)
            val dotColor = style.foreground.copy(alpha = PlanUITokens.patternDotOpacity)
            val hatchColor = style.foreground.copy(alpha = PlanUITokens.patternHatchOpacity)
            onDrawBehind {
                when (style.pattern) {
                    TimelinePattern.DOTS -> drawPath(pattern, dotColor)
                    TimelinePattern.HATCH -> drawPath(pattern, hatchColor, style = hatchStroke)
                    null -> Unit
                }
                if (style.dashed) {
                    drawRoundRect(
                        style.foreground,
                        Offset(inset, inset),
                        borderSize,
                        radius,
                        style = dashedStroke,
                    )
                }
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        if (glyph != null) {
            Icon(
                glyph,
                null,
                Modifier.size(
                    if (size ==
                        CategorySwatchSize.Small
                    ) {
                        CategorySwatchMetrics.SMALL_GLYPH.dp
                    } else {
                        PlanUITokens.space4.dp
                    },
                ),
                tint = style.foreground,
            )
        }
    }
}

private fun glyph(glyph: TimelineGlyph) =
    when (glyph) {
        TimelineGlyph.MOON -> LucideGlyphs.Moon
        TimelineGlyph.BED -> LucideGlyphs.Bed
        TimelineGlyph.ARMCHAIR -> LucideGlyphs.Armchair
        TimelineGlyph.SUN -> LucideGlyphs.Sun
        TimelineGlyph.GLASSES -> LucideGlyphs.Glasses
        TimelineGlyph.CLOUD_SUN -> LucideGlyphs.CloudSun
        TimelineGlyph.LAMP -> LucideGlyphs.Lamp
        TimelineGlyph.COFFEE -> LucideGlyphs.Coffee
        TimelineGlyph.PLANE -> LucideGlyphs.Plane
        TimelineGlyph.TIMER -> LucideGlyphs.Timer
        TimelineGlyph.ARROW_LEFT_RIGHT -> LucideGlyphs.ArrowLeftRight
        TimelineGlyph.FOOTPRINTS -> LucideGlyphs.Footprints
        TimelineGlyph.GLOBE -> LucideGlyphs.Globe
        TimelineGlyph.INFO -> LucideGlyphs.Info
        TimelineGlyph.CHECK -> LucideGlyphs.Check
        TimelineGlyph.SKIP_FORWARD -> LucideGlyphs.SkipForward
    }
