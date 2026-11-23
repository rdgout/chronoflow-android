package net.chronoflow.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.layout.BoardMetrics

@BoardMetrics("Main", "AuthStates", "SetStates", "SetHome", "Set2FA")
private object LucideMetrics {
    // Inline board SVGs and the bundle lucide glyphs share this canvas and default stroke.
    const val VIEWPORT = 24f
    const val STROKE_WIDTH = 2f
}

private val vectors = mutableMapOf<Pair<LucideGlyph, Float>, ImageVector>()

fun LucideGlyph.imageVector(strokeWidth: Float = LucideMetrics.STROKE_WIDTH): ImageVector =
    synchronized(vectors) {
        vectors.getOrPut(this to strokeWidth) {
            ImageVector
                .Builder(
                    name,
                    LucideMetrics.VIEWPORT.dp,
                    LucideMetrics.VIEWPORT.dp,
                    LucideMetrics.VIEWPORT,
                    LucideMetrics.VIEWPORT,
                ).apply {
                    paths.forEach { path ->
                        addPath(
                            addPathNodes(path.d),
                            fill = if (path.filled) SolidColor(Color.Black) else null,
                            stroke = SolidColor(Color.Black),
                            strokeLineWidth = strokeWidth,
                            strokeLineCap = StrokeCap.Round,
                            strokeLineJoin = StrokeJoin.Round,
                        )
                    }
                }.build()
        }
    }
