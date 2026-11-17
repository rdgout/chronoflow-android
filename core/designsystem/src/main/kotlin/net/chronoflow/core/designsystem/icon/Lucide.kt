package net.chronoflow.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.generated.WelcomeTokens

private val vectors = mutableMapOf<Pair<LucideGlyph, Float>, ImageVector>()

fun LucideGlyph.imageVector(strokeWidth: Float = WelcomeTokens.moonStroke): ImageVector =
    synchronized(vectors) {
        vectors.getOrPut(this to strokeWidth) {
            ImageVector
                .Builder(
                    name,
                    WelcomeTokens.moonViewBox.dp,
                    WelcomeTokens.moonViewBox.dp,
                    WelcomeTokens.moonViewBox,
                    WelcomeTokens.moonViewBox,
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
