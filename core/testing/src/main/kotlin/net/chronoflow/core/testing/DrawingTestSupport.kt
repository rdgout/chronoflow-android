package net.chronoflow.core.testing

import net.chronoflow.core.plan.drawing.DayDrawing
import net.chronoflow.core.plan.drawing.DrawOp
import net.chronoflow.core.plan.drawing.HitIndex
import net.chronoflow.core.plan.drawing.TimelineGlyph
import net.chronoflow.core.plan.drawing.TimelineTextStyle
import java.util.Locale

class DecodedOp(
    val code: Byte,
    val coords: FloatArray,
    val colors: IntArray,
    val args: IntArray,
    val text: String?,
    val style: Int,
    val glyph: Int,
) {
    val name: String get() = DrawOp.name(code)
}

private fun coordCount(
    code: Byte,
    n: Int,
): Int =
    when (code) {
        DrawOp.RECT -> 4
        DrawOp.LINES -> 1 + 4 * n
        DrawOp.TILE_BEGIN -> 6
        DrawOp.CORE_BAR -> 1
        DrawOp.OUTLINE_TILE -> 4
        DrawOp.GLYPH -> 3
        DrawOp.LINE, DrawOp.DASH_LINE -> 5
        DrawOp.TEXT -> 3
        DrawOp.RRECT_FILL -> 5
        DrawOp.RRECT_STROKE -> 6
        DrawOp.CIRCLE -> 3
        else -> 0
    }

private fun colorCount(code: Byte): Int =
    when (code) {
        DrawOp.CLIP_TILE, DrawOp.TILE_END, DrawOp.TILE_BEGIN, DrawOp.HATCH_TILE -> 0
        else -> 1
    }

private fun argCount(code: Byte): Int =
    when (code) {
        DrawOp.LINES, DrawOp.HATCH_TILE, DrawOp.OUTLINE_TILE, DrawOp.LINE, DrawOp.DASH_LINE -> 1
        else -> 0
    }

/** Reads the op list with forward cursors only and fails unless every operand array is consumed exactly to its end. */
fun DayDrawing.decode(): List<DecodedOp> {
    val out = ArrayList<DecodedOp>(ops.size)
    var ci = 0
    var coli = 0
    var ai = 0
    var ti = 0
    var gi = 0
    for (code in ops) {
        val args0 = if (argCount(code) == 1) intArrayOf(args[ai]) else IntArray(0)
        val n = if (code == DrawOp.LINES) args0[0] else 0
        val cc = coordCount(code, n)
        val coords = coords.copyOfRange(ci, ci + cc)
        val colors = colors.copyOfRange(coli, coli + colorCount(code))
        ci += cc
        coli += colors.size
        ai += args0.size
        var text: String? = null
        var style = -1
        var glyph = -1
        if (code == DrawOp.TEXT) {
            text = texts[ti]
            style = textStyles[ti].toInt()
            ti++
        }
        if (code == DrawOp.GLYPH) glyph = glyphs[gi++].toInt()
        out.add(DecodedOp(code, coords, colors, args0, text, style, glyph))
    }
    check(ci == coords.size) { "coords not consumed: $ci of ${coords.size}" }
    check(coli == colors.size) { "colors not consumed: $coli of ${colors.size}" }
    check(ai == args.size) { "args not consumed: $ai of ${args.size}" }
    check(ti == texts.size && ti == textStyles.size) { "texts not consumed: $ti of ${texts.size}" }
    check(gi == glyphs.size) { "glyphs not consumed: $gi of ${glyphs.size}" }
    return out
}

fun DayDrawing.serialize(): String =
    decode().joinToString("\n", postfix = "\n") { op ->
        buildString {
            append(op.name)
            if (op.coords.isNotEmpty()) {
                append(" c=").append(op.coords.joinToString(",") { String.format(Locale.ROOT, "%.2f", it) })
            }
            if (op.colors.isNotEmpty()) {
                append(" k=").append(op.colors.joinToString(",") { String.format(Locale.ROOT, "#%08X", it) })
            }
            if (op.args.isNotEmpty()) append(" a=").append(op.args.joinToString(","))
            if (op.text !=
                null
            ) {
                append(" t=\"").append(op.text).append("\" s=").append(TimelineTextStyle.entries[op.style])
            }
            if (op.glyph >= 0) append(" g=").append(TimelineGlyph.all[op.glyph].name)
        }
    }

fun opListEquals(
    a: DayDrawing,
    b: DayDrawing,
): Boolean =
    a.ops.contentEquals(b.ops) && a.coords.contentEquals(b.coords) && a.colors.contentEquals(b.colors) &&
        a.args.contentEquals(b.args) && a.texts.contentEquals(b.texts) && a.textStyles.contentEquals(b.textStyles) &&
        a.glyphs.contentEquals(b.glyphs) && a.dividerFrames.contentEquals(b.dividerFrames)

/** The ops from [from] up to (excluding) [to] as a drawing of their own, for reference renders that wrap a tile. */
fun DayDrawing.slice(
    from: Int,
    to: Int,
): DayDrawing {
    var ci = 0
    var coli = 0
    var ai = 0
    var ti = 0
    var gi = 0
    val start = IntArray(5)
    for ((index, code) in ops.withIndex()) {
        if (index == from) {
            start[0] = ci
            start[1] = coli
            start[2] = ai
            start[3] = ti
            start[4] = gi
        }
        if (index == to) break
        val n = if (code == DrawOp.LINES) args[ai] else 0
        ci += coordCount(code, n)
        coli += colorCount(code)
        ai += argCount(code)
        if (code == DrawOp.TEXT) ti++
        if (code == DrawOp.GLYPH) gi++
    }
    if (to >= ops.size) {
        ci = coords.size
        coli = colors.size
        ai = args.size
        ti = texts.size
        gi = glyphs.size
    }
    return DayDrawing(
        key,
        widthPx,
        heightPx,
        ops.copyOfRange(from, minOf(to, ops.size)),
        coords.copyOfRange(start[0], ci),
        colors.copyOfRange(start[1], coli),
        args.copyOfRange(start[2], ai),
        texts.copyOfRange(start[3], ti),
        textStyles.copyOfRange(start[3], ti),
        glyphs.copyOfRange(start[4], gi),
        FloatArray(0),
        emptyList(),
        HitIndex.Empty,
    )
}
