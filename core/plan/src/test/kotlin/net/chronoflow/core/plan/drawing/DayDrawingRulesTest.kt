package net.chronoflow.core.plan.drawing

import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.Priority
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.plan.generated.TimelinePalette
import net.chronoflow.core.testing.DecodedOp
import net.chronoflow.core.testing.decode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DayDrawingRulesTest {
    private val palette = TimelinePalette.Light

    private fun tileOps(d: DayDrawing): List<DecodedOp> {
        val ops = d.decode()
        val from = ops.indexOfFirst { it.code == DrawOp.TILE_BEGIN }
        val to = ops.indexOfFirst { it.code == DrawOp.TILE_END }
        assertTrue("no tile", from >= 0 && to > from)
        return ops.subList(from, to + 1)
    }

    private fun fixture(
        type: BlockType = BlockType.Sleep,
        w: Double = 120.0,
        h: Double = 80.0,
        cols: Int = 1,
        tiny: Boolean = false,
        compact: Boolean = false,
        cb: Boolean = false,
        ca: Boolean = false,
        priority: Priority = Priority.Recommended,
        status: CheckStatus? = null,
        reason: String? = null,
    ) = DrawingFixtures.singleTile(
        type = type,
        tileWidth = w,
        tileHeight = h,
        cols = cols,
        tiny = tiny,
        compact = compact,
        continuesBefore = cb,
        continuesAfter = ca,
        priority = priority,
        status = status,
        reason = reason,
    )

    private fun ops(f: DrawingFixtures.SingleTile) = tileOps(f.build())

    @Test fun caffeineServingGetsHalfShortSideRadiusAndOneCentredText() {
        val ops = ops(fixture(BlockType.CaffeineServing, 100.0, 40.0))
        val begin = ops.first { it.code == DrawOp.TILE_BEGIN }
        assertEquals(20f, begin.coords[4], 0.01f)
        assertEquals(20f, begin.coords[5], 0.01f)
        assertEquals(1, ops.count { it.code == DrawOp.TEXT })
        assertTrue(ops.first { it.code == DrawOp.TEXT }.text!!.contains(" · "))
    }

    @Test fun continuedEdgesGetNoRadiusAndTwoChevronLines() {
        val before = ops(fixture(cb = true))
        assertEquals(0f, before.first { it.code == DrawOp.TILE_BEGIN }.coords[4], 0f)
        assertEquals(2, before.count { it.code == DrawOp.LINE })
        val after = ops(fixture(ca = true))
        assertEquals(0f, after.first { it.code == DrawOp.TILE_BEGIN }.coords[5], 0f)
        assertEquals(2, after.count { it.code == DrawOp.LINE })
        assertEquals(4, ops(fixture(cb = true, ca = true)).count { it.code == DrawOp.LINE })
    }

    @Test fun detailsNeedMoreThanTheThresholdAndASingleColumn() {
        assertEquals(0, ops(fixture(h = 70.0)).count { it.code == DrawOp.RRECT_FILL })
        assertEquals(1, ops(fixture(h = 77.0)).count { it.code == DrawOp.RRECT_FILL })
        assertEquals(0, ops(fixture(h = 77.0, cols = 2, w = 100.0)).count { it.code == DrawOp.RRECT_FILL })
    }

    @Test fun theIntervalIsAbsentWhenItDoesNotFit() {
        val lineHeight = PlanMetrics.laneLabelLineHeight
        val intervalTop = PlanMetrics.tilePadding + lineHeight + PlanMetrics.smallGap
        val tight = intervalTop + lineHeight - 1 + PlanMetrics.tilePadding
        val roomy = intervalTop + lineHeight + PlanMetrics.tilePadding
        assertEquals(1, ops(fixture(h = tight, w = 200.0)).count { it.code == DrawOp.TEXT })
        assertEquals(2, ops(fixture(h = roomy, w = 200.0)).count { it.code == DrawOp.TEXT })
    }

    @Test fun noCaffeineGetsTheStrikeAndACaffeineOutline() {
        val ops = ops(fixture(BlockType.NoCaffeine))
        val outline = ops.first { it.code == DrawOp.OUTLINE_TILE }
        assertEquals(2f, outline.coords[1], 0f)
        assertEquals(DrawOp.DASH_LONG, outline.args[0])
        assertEquals(palette.catCaffeine, outline.colors[0])
        assertEquals(1, ops.count { it.code == DrawOp.LINE && it.coords[4] == 2f })
    }

    @Test fun optionalOutlinesFollowTheTypeWidth() {
        val noCaffeine =
            ops(fixture(BlockType.NoCaffeine, priority = Priority.Optional)).first {
                it.code ==
                    DrawOp.OUTLINE_TILE
            }
        assertEquals(2f, noCaffeine.coords[1], 0f)
        assertEquals(DrawOp.DASH_LONG, noCaffeine.args[0])
        assertEquals(255 * 0.5, (noCaffeine.colors[0] ushr 24).toDouble(), 1.0)
        val sleep = ops(fixture(BlockType.Sleep, priority = Priority.Optional)).first { it.code == DrawOp.OUTLINE_TILE }
        assertEquals(1f, sleep.coords[1], 0f)
        assertEquals(DrawOp.DASH_SHORT, sleep.args[0])
    }

    @Test fun outlineRowsForLightOkNoteAndUnknown() {
        val lightOk = ops(fixture(BlockType.LightOK)).first { it.code == DrawOp.OUTLINE_TILE }
        assertEquals(DrawOp.DASH_SHORT, lightOk.args[0])
        val note = ops(fixture(BlockType.Note)).first { it.code == DrawOp.OUTLINE_TILE }
        assertEquals(DrawOp.DASH_SOLID, note.args[0])
        assertEquals(palette.border, note.colors[0])
        assertEquals(0, ops(fixture(BlockType.Unknown("x"))).count { it.code == DrawOp.OUTLINE_TILE })
    }

    @Test fun markedTilesPrecompositeTextStatusGlyphAndIconColors() {
        val ops = ops(fixture(BlockType.Sleep, status = CheckStatus.Done))

        fun pre(c: Int) = TimelineColors.precomposite(c, 0.6, palette.surface2)
        val texts = ops.filter { it.code == DrawOp.TEXT }
        assertTrue(texts.isNotEmpty())
        assertTrue(texts.all { it.colors[0] == pre(palette.cardForeground) })
        val glyphs = ops.filter { it.code == DrawOp.GLYPH }
        assertEquals(2, glyphs.size)
        assertEquals(pre(palette.catSleepFg), glyphs[0].colors[0])
        assertEquals(TimelineGlyph.CHECK.ordinal, glyphs[1].glyph)
        assertEquals(pre(palette.foreground), glyphs[1].colors[0])
    }

    @Test fun unknownTypeDrawsNoGlyphAndUnknownStatusDrawsSkipForward() {
        val ops = ops(fixture(BlockType.Unknown("x"), status = CheckStatus.Unknown("y")))
        val glyphs = ops.filter { it.code == DrawOp.GLYPH }
        assertEquals(listOf(TimelineGlyph.SKIP_FORWARD.ordinal), glyphs.map { it.glyph })
    }

    @Test fun clipAppearsOnlyWhenContentLeavesTheTileShape() {
        assertFalse(ops(fixture(w = 120.0, h = 80.0)).any { it.code == DrawOp.CLIP_TILE })
        assertFalse(ops(fixture(BlockType.CaffeineServing, 30.0, 20.0)).any { it.code == DrawOp.CLIP_TILE })
        assertTrue(ops(fixture(h = 10.0, tiny = true)).any { it.code == DrawOp.CLIP_TILE })
        assertTrue(
            ops(fixture(BlockType.CaffeineServing, 16.0, 16.0, compact = true)).any { it.code == DrawOp.CLIP_TILE },
        )
        assertTrue(ops(fixture(w = 16.0, h = 120.0, cols = 3)).any { it.code == DrawOp.CLIP_TILE })
    }

    @Test fun aRunNarrowerThanTheEllipsisIsDroppedWithoutAPlate() {
        val ops = ops(fixture(w = 35.0, h = 120.0))
        assertEquals("only the interval keeps its plate", 1, ops.count { it.code == DrawOp.RECT })
        val texts = ops.filter { it.code == DrawOp.TEXT }
        assertTrue(texts.isNotEmpty())
        assertFalse(texts.any { it.text == "Title" })
    }

    @Test fun compactDrawsNoTextInsideAnyTile() {
        val fixture = DrawingFixtures.prepared("S3")
        for (day in fixture.plan.days) {
            val drawing =
                DrawingFixtures.build(
                    day,
                    fixture.layouts.getValue(day.id),
                    DrawingFixtures.key(day, "r", 411),
                )
            var inside = false
            for (op in drawing.decode()) {
                if (op.code == DrawOp.TILE_BEGIN) inside = true
                if (op.code == DrawOp.TILE_END) inside = false
                assertFalse("text in a compact tile of ${day.id}", inside && op.code == DrawOp.TEXT)
            }
        }
    }

    @Test fun coreTilesGetTheCoreBarAndReasonRowsFollowTheTag() {
        val ops = ops(fixture(priority = Priority.Core, h = 120.0, reason = "Because"))
        assertEquals(1, ops.count { it.code == DrawOp.CORE_BAR })
        val texts = ops.filter { it.code == DrawOp.TEXT }.map { it.text }
        assertTrue(texts.contains("Because"))
    }

    @Test fun everyOpListWalksToTheEndOfEveryArray() {
        val fixture = DrawingFixtures.prepared("S3")
        for (day in fixture.plan.days) {
            DrawingFixtures.build(day, fixture.layouts.getValue(day.id), DrawingFixtures.key(day, "r", 841)).decode()
        }
    }
}
