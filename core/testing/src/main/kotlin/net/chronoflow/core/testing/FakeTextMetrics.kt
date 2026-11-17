package net.chronoflow.core.testing

import net.chronoflow.core.plan.drawing.TextMetrics
import net.chronoflow.core.plan.drawing.TimelineTextStyle

/** Deterministic metrics: 0.55 em per character (0.6 em for tick labels), ascent 0.8 em; ellipsis mirrors `TextUtils`. */
class FakeTextMetrics : TextMetrics {
    override fun measure(
        style: TimelineTextStyle,
        text: String,
        sizePx: Float,
    ): Float = text.length * advance(style) * sizePx

    override fun ellipsize(
        style: TimelineTextStyle,
        text: String,
        sizePx: Float,
        maxWidthPx: Float,
    ): String {
        if (measure(style, text, sizePx) <= maxWidthPx) return text
        var length = text.length
        while (length > 0 && measure(style, text.substring(0, length) + ELLIPSIS, sizePx) > maxWidthPx) length--
        return if (length == 0) "" else text.substring(0, length) + ELLIPSIS
    }

    override fun ascent(
        style: TimelineTextStyle,
        sizePx: Float,
    ): Float = 0.8f * sizePx

    private fun advance(style: TimelineTextStyle): Float = if (style == TimelineTextStyle.TICK) 0.6f else 0.55f

    private companion object {
        const val ELLIPSIS = "…"
    }
}
