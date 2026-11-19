package net.chronoflow.feature.trip.timeline

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import androidx.test.core.app.ApplicationProvider
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.Priority
import net.chronoflow.core.plan.drawing.DrawOp
import net.chronoflow.core.plan.drawing.TimelineColors
import net.chronoflow.core.plan.drawing.TimelineTextStyle
import net.chronoflow.core.plan.generated.TimelinePalette
import net.chronoflow.core.testing.decode
import net.chronoflow.core.testing.slice
import net.chronoflow.core.testing.tiers.Layout
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The pre-composited build against the same tile drawn raw inside a real `saveLayerAlpha`. This is the only
 * `saveLayer` in the codebase; it exists to prove the pre-composite rule, never in `src/main` (A12).
 */
@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class PrecompositedAlphaTest(
    private val type: BlockType,
    private val marked: Boolean,
    @Suppress("unused") private val label: String,
) {
    private val context: Context = ApplicationProvider.getApplicationContext()

    /** Shapes, patterns, glyphs and outlines: no text, so only double 8-bit rounding may differ (OQ-1c-9). */
    @Test fun shapesMatchTheSaveLayerReference() = compare(compact = true)

    /**
     * With text, Skia's glyph contrast pre-blend depends on the text colour, so glyph edge pixels differ more
     * between a pre-composited colour and a layer. That looser tolerance holds only inside the TEXT line boxes, and
     * the share of pixels beyond the shape tolerance is measured against the tile area.
     */
    @Test fun textEdgesStayWithinTheGlyphContrastTolerance() = compare(compact = false)

    private fun compare(compact: Boolean) {
        val typefaces = TimelineTestFonts.typefaces(context)
        val tile =
            TimelineTestPlans.singleTile(
                type = type,
                priority = if (marked) Priority.Core else Priority.Optional,
                status = if (marked) CheckStatus.Done else null,
                top = 10.0,
                height = 120.0,
                cols = 1,
                window = 640,
                compact = compact,
            )
        val palette = TimelinePalette.Light
        val resources = TimelineResources(TimelineTestPlans.DENSITY, false, palette, typefaces)
        val metrics = TextPaintMetrics(typefaces)
        val pre = TimelineTestPlans.build(tile.day, tile.layout, tile.key, metrics, precomposite = true)
        val raw = TimelineTestPlans.build(tile.day, tile.layout, tile.key, metrics, precomposite = false)
        val a = Bitmap.createBitmap(pre.widthPx, pre.heightPx, Bitmap.Config.ARGB_8888)
        TimelineTestPlans.render(pre, resources, a, palette.background)

        val ops = raw.decode()
        val begin = ops.indexOfFirst { it.code == DrawOp.TILE_BEGIN }
        val end = ops.indexOfFirst { it.code == DrawOp.TILE_END }
        val frame = ops[begin].coords
        val alpha =
            TimelineColors.groupAlpha(
                if (marked) CheckStatus.Done else null,
                if (marked) Priority.Core else Priority.Optional,
            )
        val b = Bitmap.createBitmap(pre.widthPx, pre.heightPx, Bitmap.Config.ARGB_8888)
        b.eraseColor(palette.background)
        val canvas = Canvas(b)
        replayDayDrawing(canvas, raw.slice(0, begin), resources)
        val bounds = RectF(frame[0] - 2, frame[1] - 2, frame[0] + frame[2] + 2, frame[1] + frame[3] + 2)
        canvas.saveLayerAlpha(bounds, (alpha * 255).roundToInt())
        replayDayDrawing(canvas, raw.slice(begin, end + 1), resources)
        canvas.restore()

        // TEXT coordinates are tile-local; the replay translates the canvas to the tile origin.
        val textBoxes =
            ops.filter { it.code == DrawOp.TEXT }.map {
                val size = it.coords[2]
                val width = metrics.measure(TimelineTextStyle.entries[it.style], it.text.orEmpty(), size)
                val x = frame[0] + it.coords[0]
                val baseline = frame[1] + it.coords[1]
                RectF(x - 2, baseline - size - 2, x + width + 2, baseline + size * 0.5f + 2)
            }
        var worst = 0
        var worstOutsideText = 0
        var off = 0
        var textOver3 = 0
        val pa = IntArray(a.width * a.height)
        val pb = IntArray(pa.size)
        a.getPixels(pa, 0, a.width, 0, 0, a.width, a.height)
        b.getPixels(pb, 0, b.width, 0, 0, b.width, b.height)
        for (i in pa.indices) {
            if (pa[i] == pb[i]) continue
            var diff = 0
            for (shift in intArrayOf(0, 8, 16, 24)) {
                diff =
                    maxOf(diff, abs(((pa[i] shr shift) and 0xFF) - ((pb[i] shr shift) and 0xFF)))
            }
            worst = maxOf(worst, diff)
            if (diff > 1) off++
            val x = (i % a.width).toFloat() + 0.5f
            val y = (i / a.width).toFloat() + 0.5f
            if (textBoxes.any { it.contains(x, y) }) {
                if (diff > SHAPE_TOLERANCE) textOver3++
            } else {
                worstOutsideText = maxOf(worstOutsideText, diff)
            }
        }
        println(
            "PrecompositedAlpha metric: $label compact=$compact maxChannelDiff=$worst pixelsOver1=$off of ${pa.size}",
        )
        val tileArea = frame[2] * frame[3]
        assertTrue("$label differs by $worst/255", worst <= TEXT_TOLERANCE)
        assertTrue("$label differs by $worstOutsideText/255 outside text", worstOutsideText <= SHAPE_TOLERANCE)
        assertTrue(
            "$label: $textOver3 text pixels differ by more than $SHAPE_TOLERANCE/255 (tile area ${tileArea.roundToInt()})",
            textOver3 <= TEXT_SHARE * tileArea,
        )
    }

    companion object {
        private const val SHAPE_TOLERANCE = 3
        private const val TEXT_TOLERANCE = 20
        private const val TEXT_SHARE = 0.05

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{2}")
        fun cases(): List<Array<Any>> =
            BlockType.known.flatMap { type ->
                listOf(true, false).map { marked ->
                    arrayOf<Any>(type, marked, "${type.raw} ${if (marked) "marked" else "optional"}")
                }
            }
    }
}
