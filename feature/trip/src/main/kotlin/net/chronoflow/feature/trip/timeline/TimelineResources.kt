package net.chronoflow.feature.trip.timeline

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import androidx.compose.runtime.Stable
import net.chronoflow.core.plan.drawing.TimelineColors
import net.chronoflow.core.plan.drawing.TimelineTextStyle
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.plan.generated.TimelinePalette
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * Paints, pattern shaders, glyph paths and scratch paths shared by every day drawing of one (density, dark). Created
 * once, on main or at setup, never inside a draw; the replay allocates nothing. Not thread-safe: the render thread owns it.
 */
@Stable
class TimelineResources(
    val density: Float,
    val dark: Boolean,
    palette: TimelinePalette,
    private val typefaces: TimelineTypefaces? = null,
) {
    internal val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    internal val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    internal val roundStroke =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
    internal val dashShort = dashed(3f * density, 3f * density, Paint.Cap.BUTT)
    internal val dashLong = dashed(6f * density, 6f * density, Paint.Cap.BUTT)
    internal val dashZone = dashed(2f * density, 4f * density, Paint.Cap.ROUND)
    internal val glyphStroke =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = GLYPH_STROKE_UNITS
        }
    internal val glyphFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    internal val dotPaint =
        Paint(
            Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG,
        ).apply { shader = dotShader(density) }
    internal val hatchPaints: Array<Paint> =
        TimelineColors.hatchColorTable(palette).let { table ->
            Array(table.size) { Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = hatchShader(density, table[it]) } }
        }
    internal val textPaints: Array<Paint> =
        Array(TimelineTextStyle.entries.size) { index ->
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                isSubpixelText = true
                if (index == TimelineTextStyle.TICK.ordinal) fontFeatureSettings = TABULAR_FIGURES
            }
        }
    internal val tilePath = Path()
    internal val outlinePath = Path()
    internal val glyphPaths: Array<Array<Path>> get() = TimelineGlyphPaths.paths
    internal val glyphFilled: Array<BooleanArray> get() = TimelineGlyphPaths.filled

    /** The typeface is resolved at replay: a drawing is measured with the loaded Figtree, which may load after composition. */
    internal fun textPaint(style: Int): Paint {
        val paint = textPaints[style]
        val loaded = typefaces ?: TimelineTypefaces.cachedOrNull()
        if (loaded != null) {
            val face = loaded.faceFor(style)
            if (paint.typeface !== face) paint.typeface = face
        }
        return paint
    }

    private fun dashed(
        on: Float,
        off: Float,
        cap: Paint.Cap,
    ) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = cap
        pathEffect = DashPathEffect(floatArrayOf(on, off), 0f)
    }

    /** One `main`-thread map of resources per (density, dark). */
    object Cache {
        private data class Key(
            val density: Float,
            val dark: Boolean,
        )

        private val entries = HashMap<Key, TimelineResources>()

        fun get(
            density: Float,
            dark: Boolean,
            palette: TimelinePalette,
        ): TimelineResources = entries.getOrPut(Key(density, dark)) { TimelineResources(density, dark, palette) }
    }

    internal companion object {
        const val GLYPH_STROKE_UNITS = 2f

        /** An ALPHA_8 cell of one dot, tiled with a 7 dp period, so the paint colour tints it. */
        fun dotShader(density: Float): Shader {
            val period = PlanMetrics.dotStep.toFloat()
            val cell = ceil(period * density).toInt()
            val bitmap = Bitmap.createBitmap(cell, cell, Bitmap.Config.ALPHA_8)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = OPAQUE }
            Canvas(bitmap).drawCircle(cell / 2f, cell / 2f, cell / period * PlanMetrics.dotRadius.toFloat(), paint)
            return BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT).apply {
                setLocalMatrix(Matrix().apply { setScale(period * density / cell, period * density / cell) })
            }
        }

        /** The board CSS: a 2 dp "/" stripe from the tile's top-left corner every 8 dp (OQ-1c-2). */
        fun hatchShader(
            density: Float,
            color: Int,
        ): Shader {
            val reach = (PlanMetrics.patternStep / sqrt(2.0)).toFloat() * density
            val stripe = (PlanMetrics.patternLine / PlanMetrics.patternStep).toFloat()
            return LinearGradient(
                0f,
                0f,
                reach,
                reach,
                intArrayOf(color, color, TimelineColors.TRANSPARENT, TimelineColors.TRANSPARENT),
                floatArrayOf(0f, stripe, stripe, 1f),
                Shader.TileMode.REPEAT,
            )
        }

        private const val OPAQUE = -1
    }
}
