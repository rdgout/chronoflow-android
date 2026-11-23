package net.chronoflow.core.plan.drawing

enum class TimelineTextStyle { TICK, TITLE, TAG }

/** Text measurement behind the builder: `TextPaint` on Android, a deterministic fake in JVM tests. Not thread-safe. */
interface TextMetrics {
    fun measure(
        style: TimelineTextStyle,
        text: String,
        sizePx: Float,
    ): Float

    /** Truncates the end with U+2026 and returns [text] unchanged when it fits. */
    fun ellipsize(
        style: TimelineTextStyle,
        text: String,
        sizePx: Float,
        maxWidthPx: Float,
    ): String

    /** Positive distance from the line top to the baseline. */
    fun ascent(
        style: TimelineTextStyle,
        sizePx: Float,
    ): Float
}
