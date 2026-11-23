package net.chronoflow.feature.trip.timeline

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.plan.drawing.DayDrawing
import net.chronoflow.core.plan.drawing.DrawOp
import net.chronoflow.core.plan.drawing.PaletteSlot
import net.chronoflow.core.plan.drawing.TimelineColors
import net.chronoflow.core.plan.drawing.TimelineTextStyle
import net.chronoflow.core.plan.drawing.color
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.testing.decode
import net.chronoflow.core.testing.tiers.Layout
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs
import kotlin.math.roundToInt

/** S3 replayed in every window, language, theme and font scale: text stays in its tile, tiles in their lanes, pixels in the token set. */
@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class ReplayGuardTest(
    private val window: Int,
    private val language: Language,
    private val dark: Boolean,
    private val fontScale: Float,
    @Suppress("unused") private val label: String,
) {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val density = TimelineTestPlans.DENSITY

    @Test fun replayKeepsEveryRunInItsTileAndEveryTileInItsLane() {
        val fixture = TimelineTestPlans.prepared("S3", language)
        val typefaces = TimelineTestFonts.typefaces(context)
        val palette = TimelineTestPlans.palette(dark)
        val resources = TimelineResources(density, dark, palette, typefaces)
        val metrics = TextPaintMetrics(typefaces)
        val drawings =
            fixture.plan.days.map { day ->
                val key = TimelineTestPlans.key(day, fixture.plan.revision, window, density, dark, language, fontScale)
                TimelineTestPlans.build(day, fixture.layouts.getValue(day.id), key, metrics) to key.textScale
            }
        val width = drawings.maxOf { it.first.widthPx }
        val height = drawings.maxOf { it.first.heightPx }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        val allowed = allowedColors(dark)
        var tileText = 0
        for ((drawing, scale) in drawings) {
            val lineHeight = (PlanMetrics.laneLabelLineHeight * density * scale).toFloat()
            tileText += checkGeometry(drawing, metrics, lineHeight)
            assertTrue("a colour outside the token set", drawing.colors.all { it in allowed })
            TimelineTestPlans.render(drawing, resources, bitmap, palette.background)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            checkUnpainted(drawing, pixels, width, palette.background, palette.surface2, metrics, lineHeight)
        }
        if (window < PlanMetrics.timelineLabelBreakpoint) {
            assertTrue("compact windows draw no tile text", tileText == 0)
        } else {
            assertTrue("wide windows draw tile text", tileText > 0)
        }
    }

    private fun checkGeometry(
        drawing: DayDrawing,
        metrics: TextPaintMetrics,
        lineHeight: Float,
    ): Int {
        val ops = drawing.decode()
        val lanes = ops.filter { it.code == DrawOp.RECT }.take(4).map { it.coords }
        var tile: FloatArray? = null
        var count = 0
        for (op in ops) {
            when (op.code) {
                DrawOp.TILE_BEGIN -> {
                    val frame = op.coords
                    tile = frame
                    val inLane = lanes.any { frame[0] >= it[0] - EPS && frame[0] + frame[2] <= it[2] + EPS }
                    assertTrue("a tile leaves its lane: ${frame.toList()}", inLane)
                }

                DrawOp.TILE_END -> {
                    tile = null
                }

                DrawOp.TEXT -> {
                    val style = TimelineTextStyle.entries[op.style]
                    val size = op.coords[2]
                    val measured = metrics.measure(style, op.text!!, size)
                    val left = op.coords[0]
                    val top = op.coords[1] - metrics.ascent(style, size)
                    val frame = tile
                    if (frame != null) {
                        count++
                        assertTrue(
                            "\"${op.text}\" leaves its tile ${frame.toList()}: $left..${left + measured} x $top..${top + lineHeight}",
                            left >= -EPS && left + measured <= frame[2] + EPS && top >= -EPS &&
                                top + lineHeight <= frame[3] + EPS,
                        )
                    } else if (style == TimelineTextStyle.TICK) {
                        assertTrue(
                            "tick \"${op.text}\" leaves the gutter",
                            left >= -EPS && left + measured <= PlanMetrics.gutterWidth * density + EPS,
                        )
                    }
                }
            }
        }
        return count
    }

    private fun checkUnpainted(
        drawing: DayDrawing,
        pixels: IntArray,
        stride: Int,
        background: Int,
        surface2: Int,
        metrics: TextPaintMetrics,
        lineHeight: Float,
    ) {
        val ops = drawing.decode()
        val lanes = ops.filter { it.code == DrawOp.RECT }.take(4).map { it.coords }
        val covers = ArrayList<FloatArray>()
        var skippedLanes = 0
        var inTile = false
        for (op in ops) {
            val c = op.coords
            when (op.code) {
                DrawOp.RECT -> {
                    if (skippedLanes < 4 && !inTile) skippedLanes++
                }

                DrawOp.LINES -> {
                    val half = c[0] / 2 + 1
                    for (i in 0 until op.args[0]) {
                        val o = 1 + 4 * i
                        covers.add(
                            floatArrayOf(
                                minOf(c[o], c[o + 2]) - half,
                                minOf(c[o + 1], c[o + 3]) - half,
                                maxOf(c[o], c[o + 2]) + half,
                                maxOf(c[o + 1], c[o + 3]) + half,
                            ),
                        )
                    }
                }

                DrawOp.TILE_BEGIN -> {
                    inTile = true
                    covers.add(floatArrayOf(c[0] - 1, c[1] - 1, c[0] + c[2] + 1, c[1] + c[3] + 1))
                }

                DrawOp.TILE_END -> {
                    inTile = false
                }

                DrawOp.TEXT -> {
                    if (!inTile) {
                        val style = TimelineTextStyle.entries[op.style]
                        val top = c[1] - metrics.ascent(style, c[2])
                        covers.add(
                            floatArrayOf(
                                c[0] - 1,
                                top - 1,
                                c[0] + metrics.measure(style, op.text!!, c[2]) + 1,
                                top + lineHeight + 1,
                            ),
                        )
                    }
                }

                DrawOp.RRECT_FILL, DrawOp.RRECT_STROKE -> {
                    if (!inTile) {
                        covers.add(
                            floatArrayOf(
                                c[0] - 1,
                                c[1] - 1,
                                c[2] + 1,
                                c[3] + 1,
                            ),
                        )
                    }
                }

                DrawOp.CIRCLE -> {
                    if (!inTile) {
                        covers.add(
                            floatArrayOf(
                                c[0] - c[2] - 1,
                                c[1] - c[2] - 1,
                                c[0] + c[2] + 1,
                                c[1] + c[2] + 1,
                            ),
                        )
                    }
                }

                DrawOp.DASH_LINE -> {
                    val half = c[4] / 2 + 2
                    covers.add(
                        floatArrayOf(
                            minOf(c[0], c[2]) - half,
                            minOf(c[1], c[3]) - half,
                            maxOf(c[0], c[2]) + half,
                            maxOf(c[1], c[3]) + half,
                        ),
                    )
                }
            }
        }
        var x = 3
        var checked = 0
        while (x < drawing.widthPx - 2) {
            val px = x + 0.5f
            val nearEdge = lanes.any { abs(px - it[0]) < 2f || abs(px - it[2]) < 2f }
            if (!nearEdge) {
                val inLane = lanes.any { px > it[0] && px < it[2] }
                val expected = if (inLane) surface2 else background
                var y = 3
                while (y < drawing.heightPx - 2) {
                    val py = y + 0.5f
                    if (covers.none { px >= it[0] && px <= it[2] && py >= it[1] && py <= it[3] }) {
                        checked++
                        assertTrue(
                            "pixel ($x,$y) is ${pixels[y * stride + x].toUInt().toString(
                                16,
                            )}, expected ${expected.toUInt().toString(16)}",
                            pixels[y * stride + x] == expected,
                        )
                    }
                    y += STRIDE
                }
            }
            x += STRIDE
        }
        assertTrue("no unpainted pixel was sampled", checked > 0)
    }

    private fun allowedColors(dark: Boolean): Set<Int> {
        val palette = TimelineTestPlans.palette(dark)
        val alphas =
            doubleArrayOf(
                1.0,
                PlanMetrics.dotOpacity,
                PlanMetrics.hatchOpacity,
                PlanMetrics.coreBorderOpacity,
                PlanMetrics.tagOpacity,
                PlanMetrics.optionalBorderOpacity,
                PlanMetrics.gridOpacity,
            )
        val groups = doubleArrayOf(1.0, PlanMetrics.optionalOpacity, PlanMetrics.markedOpacity)
        val set = HashSet<Int>()
        for (slot in PaletteSlot.entries) {
            val token = palette.color(slot)
            for (group in groups) {
                val base = TimelineColors.precomposite(token, group, palette.surface2)
                for (alpha in alphas) set.add(TimelineColors.withAlpha(base, alpha))
            }
        }
        return set
    }

    companion object {
        private const val EPS = 0.5f
        private const val STRIDE = 7

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{4}")
        fun cases(): List<Array<Any>> {
            val out = ArrayList<Array<Any>>()
            for (window in listOf(360, 411, 639, 640, 841)) {
                for (language in listOf(Language.En, Language.Nl)) {
                    for (dark in listOf(false, true)) {
                        for (scale in listOf(1.0f, 1.3f)) {
                            out.add(
                                arrayOf(
                                    window,
                                    language,
                                    dark,
                                    scale,
                                    "W$window ${language.raw} ${if (dark) "dark" else "light"} x$scale",
                                ),
                            )
                        }
                    }
                }
            }
            return out
        }
    }
}
