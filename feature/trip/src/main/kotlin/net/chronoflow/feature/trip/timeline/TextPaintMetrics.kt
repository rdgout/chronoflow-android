package net.chronoflow.feature.trip.timeline

import android.graphics.Paint
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import net.chronoflow.core.plan.drawing.TextMetrics
import net.chronoflow.core.plan.drawing.TimelineTextStyle
import net.chronoflow.core.plan.generated.PlanMetrics

/** `TextPaint` measurement with the Figtree faces. One instance per build job: a `Paint` is not thread-safe. */
class TextPaintMetrics(
    typefaces: TimelineTypefaces,
) : TextMetrics {
    private val paints =
        Array(TimelineTextStyle.entries.size) { index ->
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                isSubpixelText = true
                typeface = typefaces.faceFor(index)
                if (index == TimelineTextStyle.TICK.ordinal) fontFeatureSettings = TABULAR_FIGURES
            }
        }
    private val fontMetrics = Paint.FontMetrics()

    private fun paint(
        style: TimelineTextStyle,
        sizePx: Float,
    ): TextPaint = paints[style.ordinal].also { it.textSize = sizePx }

    override fun measure(
        style: TimelineTextStyle,
        text: String,
        sizePx: Float,
    ): Float = paint(style, sizePx).measureText(text)

    override fun ellipsize(
        style: TimelineTextStyle,
        text: String,
        sizePx: Float,
        maxWidthPx: Float,
    ): String = TextUtils.ellipsize(text, paint(style, sizePx), maxWidthPx, TextUtils.TruncateAt.END).toString()

    override fun ascent(
        style: TimelineTextStyle,
        sizePx: Float,
    ): Float {
        paint(style, sizePx).getFontMetrics(fontMetrics)
        return -fontMetrics.ascent
    }
}

internal const val TABULAR_FIGURES = "'tnum' 1"

/** The 11 sp lane label resolved through [density] over its 11 dp design size: 1.0 at font scale 1, at most the 1.3 root cap. */
fun timelineTextScale(density: Density): Float =
    with(density) { PlanMetrics.laneLabelSize.sp.toPx() } / (PlanMetrics.laneLabelSize.toFloat() * density.density)
