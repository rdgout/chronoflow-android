package net.chronoflow.core.plan.drawing

/**
 * One timeline day's static content as a struct-of-arrays op list. Immutable by contract: only
 * [DayDrawingBuilder] writes the arrays and nothing exposes a mutator. See [DrawOp] for the operands.
 */
class DayDrawing(
    @JvmField val key: DayDrawingKey,
    @JvmField val widthPx: Int,
    @JvmField val heightPx: Int,
    @JvmField val ops: ByteArray,
    @JvmField val coords: FloatArray,
    @JvmField val colors: IntArray,
    @JvmField val args: IntArray,
    @JvmField val texts: Array<String>,
    @JvmField val textStyles: ByteArray,
    @JvmField val glyphs: ByteArray,
    /** Four per divider: l, t, r, b in px. */
    @JvmField val dividerFrames: FloatArray,
    @JvmField val targets: List<TimelineTarget>,
    @JvmField val hits: HitIndex,
) {
    @JvmField
    val estimatedBytes: Int =
        ops.size + 4 * (coords.size + colors.size + args.size + dividerFrames.size) + textStyles.size + glyphs.size +
            texts.sumOf { 40 + 2 * it.length } +
            targets.sumOf { 64 + 2 * (it.label.length + (it.testTag?.length ?: 0)) } +
            hits.estimatedBytes + 128
}
