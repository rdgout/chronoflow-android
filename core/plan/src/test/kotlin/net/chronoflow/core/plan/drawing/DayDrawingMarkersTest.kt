package net.chronoflow.core.plan.drawing

import net.chronoflow.core.model.contracts.Lane
import net.chronoflow.core.plan.PlacedMarker
import net.chronoflow.core.plan.ZoneDivider
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.plan.generated.TimelinePalette
import net.chronoflow.core.testing.FakeTextMetrics
import net.chronoflow.core.testing.decode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Gutter markers and zone dividers, checked against `TimelineRenderer.swift` l.404-438 at density 1. */
class DayDrawingMarkersTest {
    private val palette = TimelinePalette.Light
    private val metrics = FakeTextMetrics()

    private fun drawing(
        markerTop: Double? = null,
        dividerTop: Double? = null,
        label: String = "Singapore time from here",
    ): DayDrawing {
        val f = DrawingFixtures.singleTile(tileHeight = 300.0)
        val source =
            f.layout.placed
                .getValue(Lane.Sleep)
                .first()
        val zone =
            f.layout.day.zones
                .first()
        val layout =
            f.layout.copy(
                markers = listOfNotNull(markerTop?.let { PlacedMarker(source.block, it, it, source.presentation) }),
                dividers = listOfNotNull(dividerTop?.let { ZoneDivider(zone, zone, it) }),
            )
        val day = f.day.copy(dividerLabels = if (dividerTop != null) listOf(label) else emptyList())
        return DrawingFixtures.build(day, layout, f.key, metrics)
    }

    @Test fun anInfoLaneBlockDrawsACardCircleAndAnIGlyphBesideTheGutterEdge() {
        val top = 200.0
        val ops = drawing(markerTop = top).decode()
        val circle = ops.single { it.code == DrawOp.CIRCLE }
        // iOS: rect (gutterWidth - gutterMarkerSize, top - size/2, size, size), filled as an ellipse in `card`.
        val size = PlanMetrics.gutterMarkerSize.toFloat()
        assertEquals(PlanMetrics.gutterWidth.toFloat() - size / 2, circle.coords[0], 0.01f)
        assertEquals(top.toFloat(), circle.coords[1], 0.01f)
        assertEquals(size / 2, circle.coords[2], 0.01f)
        assertEquals(palette.card, circle.colors[0])
        val glyph = ops.last { it.code == DrawOp.TEXT }
        assertEquals("i", glyph.text)
        assertEquals(palette.foreground, glyph.colors[0])
        // iOS: the text rect is the marker rect inset by smallGap horizontally, so it starts at 24 and is 16 wide.
        val left = PlanMetrics.gutterWidth.toFloat() - size + PlanMetrics.smallGap.toFloat()
        val width = size - 2 * PlanMetrics.smallGap.toFloat()
        val advance = metrics.measure(TimelineTextStyle.TITLE, "i", glyph.coords[2])
        assertTrue(advance <= width)
        assertEquals(left, glyph.coords[0], 0.01f)
        val lineTop = glyph.coords[1] - metrics.ascent(TimelineTextStyle.TITLE, glyph.coords[2])
        assertEquals(top.toFloat() - size / 2, lineTop, 0.01f)
    }

    @Test fun aZoneDividerIsARoundDashedFullWidthLineWithAPillCenteredOnIt() {
        val top = 200f
        val d = drawing(dividerTop = top.toDouble())
        val ops = d.decode()
        val line = ops.single { it.code == DrawOp.DASH_LINE }
        assertEquals(listOf(0f, top, d.widthPx.toFloat(), top), line.coords.take(4))
        assertEquals(PlanMetrics.emphasisBorder.toFloat(), line.coords[4], 0.01f)
        assertEquals(palette.catTravel, line.colors[0])
        assertEquals(DrawOp.DASH_ZONE, line.args[0])
        val tagH = PlanMetrics.planPriorityLineHeight.toFloat()
        val pillH = tagH + 2 * PlanMetrics.zonePillVerticalPadding.toFloat()
        val pillLeft = (PlanMetrics.gutterWidth + PlanMetrics.laneGap).toFloat()
        val pill = ops.last { it.code == DrawOp.RRECT_FILL }
        assertEquals(pillLeft, pill.coords[0], 0.01f)
        assertEquals(top - pillH / 2, pill.coords[1], 0.01f)
        assertEquals(top + pillH / 2, pill.coords[3], 0.01f)
        assertEquals(pillH / 2, pill.coords[4], 0.01f)
        val text =
            metrics.measure(
                TimelineTextStyle.TAG,
                "Singapore time from here",
                PlanMetrics.planPrioritySize.toFloat(),
            )
        assertEquals(pillLeft + text + 2 * PlanMetrics.zonePillPadding.toFloat(), pill.coords[2], 0.01f)
        val label = ops.last { it.code == DrawOp.TEXT }
        assertEquals(palette.catTravelFg, label.colors[0])
        assertEquals(pillLeft + PlanMetrics.zonePillPadding.toFloat(), label.coords[0], 0.01f)
        assertEquals(pill.coords.toList(), d.dividerFrames.let { listOf(it[0], it[1], it[2], it[3], pillH / 2) })
    }
}
