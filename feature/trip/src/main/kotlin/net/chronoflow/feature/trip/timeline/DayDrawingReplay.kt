package net.chronoflow.feature.trip.timeline

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import net.chronoflow.core.plan.drawing.DayDrawing
import net.chronoflow.core.plan.drawing.DrawOp

fun DrawScope.replay(
    drawing: DayDrawing,
    resources: TimelineResources,
) = drawIntoCanvas { replayDayDrawing(it.nativeCanvas, drawing, resources) }

/**
 * Walks the op list once with a running cursor per operand array. It allocates nothing: no lambdas, iterators, boxing,
 * strings, `Path`, `Paint` or `RectF`, and it never measures.
 */
fun replayDayDrawing(
    canvas: Canvas,
    drawing: DayDrawing,
    res: TimelineResources,
) {
    val ops = drawing.ops
    val coords = drawing.coords
    val colors = drawing.colors
    val args = drawing.args
    var c = 0
    var k = 0
    var a = 0
    var t = 0
    var g = 0
    var tileW = 0f
    var tileH = 0f
    canvas.save()
    canvas.clipRect(0f, 0f, drawing.widthPx.toFloat(), drawing.heightPx.toFloat())
    for (i in ops.indices) {
        when (ops[i]) {
            DrawOp.RECT -> {
                res.fill.color = colors[k++]
                canvas.drawRect(coords[c], coords[c + 1], coords[c + 2], coords[c + 3], res.fill)
                c += 4
            }

            DrawOp.LINES -> {
                val n = args[a++]
                res.stroke.color = colors[k++]
                res.stroke.strokeWidth = coords[c]
                canvas.drawLines(coords, c + 1, 4 * n, res.stroke)
                c += 1 + 4 * n
            }

            DrawOp.TILE_BEGIN -> {
                canvas.save()
                canvas.translate(coords[c], coords[c + 1])
                tileW = coords[c + 2]
                tileH = coords[c + 3]
                buildTilePath(res.tilePath, 0f, 0f, tileW, tileH, coords[c + 4], coords[c + 5])
                c += 6
            }

            DrawOp.FILL_TILE -> {
                res.fill.color = colors[k++]
                canvas.drawPath(res.tilePath, res.fill)
            }

            DrawOp.DOTS_TILE -> {
                res.dotPaint.color = colors[k++]
                canvas.drawPath(res.tilePath, res.dotPaint)
            }

            DrawOp.HATCH_TILE -> {
                canvas.drawPath(res.tilePath, res.hatchPaints[args[a++]])
            }

            DrawOp.CORE_BAR -> {
                res.fill.color = colors[k++]
                canvas.save()
                canvas.clipRect(0f, 0f, coords[c], tileH)
                canvas.drawPath(res.tilePath, res.fill)
                canvas.restore()
                c += 1
            }

            DrawOp.OUTLINE_TILE -> {
                val inset = coords[c]
                val paint = strokePaintFor(res, args[a++])
                paint.color = colors[k++]
                paint.strokeWidth = coords[c + 1]
                buildTilePath(res.outlinePath, inset, inset, tileW - inset, tileH - inset, coords[c + 2], coords[c + 3])
                canvas.drawPath(res.outlinePath, paint)
                c += 4
            }

            DrawOp.CLIP_TILE -> {
                canvas.clipPath(res.tilePath)
            }

            DrawOp.TILE_END -> {
                canvas.restore()
            }

            DrawOp.GLYPH -> {
                val size = coords[c + 2]
                val ordinal = drawing.glyphs[g++].toInt()
                val paths = res.glyphPaths[ordinal]
                val filled = res.glyphFilled[ordinal]
                val color = colors[k++]
                canvas.save()
                canvas.translate(coords[c], coords[c + 1])
                canvas.scale(size / GLYPH_UNITS, size / GLYPH_UNITS)
                for (p in paths.indices) {
                    val paint = if (filled[p]) res.glyphFill else res.glyphStroke
                    paint.color = color
                    canvas.drawPath(paths[p], paint)
                }
                canvas.restore()
                c += 3
            }

            DrawOp.LINE -> {
                val paint = if (args[a++] == 1) res.roundStroke else res.stroke
                paint.color = colors[k++]
                paint.strokeWidth = coords[c + 4]
                canvas.drawLine(coords[c], coords[c + 1], coords[c + 2], coords[c + 3], paint)
                c += 5
            }

            DrawOp.DASH_LINE -> {
                a++
                res.dashZone.color = colors[k++]
                res.dashZone.strokeWidth = coords[c + 4]
                canvas.drawLine(coords[c], coords[c + 1], coords[c + 2], coords[c + 3], res.dashZone)
                c += 5
            }

            DrawOp.TEXT -> {
                val paint = res.textPaint(drawing.textStyles[t].toInt())
                paint.textSize = coords[c + 2]
                paint.color = colors[k++]
                canvas.drawText(drawing.texts[t], coords[c], coords[c + 1], paint)
                t++
                c += 3
            }

            DrawOp.RRECT_FILL -> {
                res.fill.color = colors[k++]
                canvas.drawRoundRect(
                    coords[c],
                    coords[c + 1],
                    coords[c + 2],
                    coords[c + 3],
                    coords[c + 4],
                    coords[c + 4],
                    res.fill,
                )
                c += 5
            }

            DrawOp.RRECT_STROKE -> {
                res.stroke.color = colors[k++]
                res.stroke.strokeWidth = coords[c + 5]
                canvas.drawRoundRect(
                    coords[c],
                    coords[c + 1],
                    coords[c + 2],
                    coords[c + 3],
                    coords[c + 4],
                    coords[c + 4],
                    res.stroke,
                )
                c += 6
            }

            DrawOp.CIRCLE -> {
                res.fill.color = colors[k++]
                canvas.drawCircle(coords[c], coords[c + 1], coords[c + 2], res.fill)
                c += 3
            }
        }
    }
    canvas.restore()
}

private const val GLYPH_UNITS = 24f

private fun strokePaintFor(
    res: TimelineResources,
    dashKind: Int,
): Paint =
    when (dashKind) {
        DrawOp.DASH_SHORT -> res.dashShort
        DrawOp.DASH_LONG -> res.dashLong
        else -> res.stroke
    }

/** Quadratic corners as iOS `tilePath` draws them; the builder already clamped both radii. */
private fun buildTilePath(
    path: Path,
    l: Float,
    t: Float,
    r: Float,
    b: Float,
    rTop: Float,
    rBottom: Float,
) {
    path.rewind()
    path.moveTo(l + rTop, t)
    path.lineTo(r - rTop, t)
    path.quadTo(r, t, r, t + rTop)
    path.lineTo(r, b - rBottom)
    path.quadTo(r, b, r - rBottom, b)
    path.lineTo(l + rBottom, b)
    path.quadTo(l, b, l, b - rBottom)
    path.lineTo(l, t + rTop)
    path.quadTo(l, t, l + rTop, t)
    path.close()
}
