package net.chronoflow.core.plan.drawing

import net.chronoflow.core.model.contracts.BlockType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineCategoryStyleTest {
    private fun check(
        type: BlockType,
        fill: PaletteSlot,
        fg: PaletteSlot,
        glyph: TimelineGlyph?,
        pattern: TimelinePattern? = null,
        dashed: Boolean = false,
    ) {
        val s = TimelineCategoryStyle(type)
        assertEquals("$type fill", fill, s.fill)
        assertEquals("$type foreground", fg, s.foreground)
        assertEquals("$type glyph", glyph, s.glyph)
        assertEquals("$type pattern", pattern, s.pattern)
        assertEquals("$type dashed", dashed, s.dashed)
    }

    @Test fun everyBlockTypeMapsPerTheCategoryTable() {
        val sleep = PaletteSlot.CatSleep to PaletteSlot.CatSleepFg
        check(BlockType.Sleep, sleep.first, sleep.second, TimelineGlyph.MOON)
        check(BlockType.Nap, sleep.first, sleep.second, TimelineGlyph.BED, TimelinePattern.DOTS)
        check(BlockType.Rest, sleep.first, sleep.second, TimelineGlyph.ARMCHAIR, TimelinePattern.HATCH)
        check(BlockType.SeekLight, PaletteSlot.CatLight, PaletteSlot.CatLightFg, TimelineGlyph.SUN)
        check(
            BlockType.AvoidLight,
            PaletteSlot.CatDark,
            PaletteSlot.CatDarkFg,
            TimelineGlyph.GLASSES,
            TimelinePattern.HATCH,
        )
        check(
            BlockType.LightOK,
            PaletteSlot.CatModerate,
            PaletteSlot.CatModerateFg,
            TimelineGlyph.CLOUD_SUN,
            TimelinePattern.DOTS,
            true,
        )
        check(BlockType.LightNeutral, PaletteSlot.CatModerate, PaletteSlot.CatModerateFg, TimelineGlyph.LAMP)
        val caffeine = PaletteSlot.CatCaffeine to PaletteSlot.CatCaffeineFg
        check(BlockType.CaffeineServing, caffeine.first, caffeine.second, TimelineGlyph.COFFEE, TimelinePattern.DOTS)
        check(BlockType.CaffeineRoutine, caffeine.first, caffeine.second, TimelineGlyph.COFFEE, TimelinePattern.HATCH)
        check(BlockType.CaffeineOK, caffeine.first, caffeine.second, TimelineGlyph.COFFEE)
        check(BlockType.NoCaffeine, PaletteSlot.Card, PaletteSlot.Foreground, TimelineGlyph.COFFEE, null, true)
        val travel = PaletteSlot.CatTravel to PaletteSlot.CatTravelFg
        check(BlockType.Flight, travel.first, travel.second, TimelineGlyph.PLANE)
        check(BlockType.Layover, travel.first, travel.second, TimelineGlyph.TIMER, TimelinePattern.HATCH)
        check(BlockType.Transfer, travel.first, travel.second, TimelineGlyph.ARROW_LEFT_RIGHT)
        check(BlockType.MovementBreak, travel.first, travel.second, TimelineGlyph.FOOTPRINTS)
        check(BlockType.ZoneChange, travel.first, travel.second, TimelineGlyph.GLOBE)
        check(BlockType.Note, PaletteSlot.Card, PaletteSlot.Foreground, TimelineGlyph.INFO)
    }

    @Test fun unknownTypesDrawNoGlyph() {
        val s = TimelineCategoryStyle(BlockType.Unknown("x"))
        assertNull(s.glyph)
        assertEquals(PaletteSlot.Card, s.fill)
        assertEquals(PaletteSlot.Foreground, s.foreground)
        assertNull(s.pattern)
        assertFalse(s.dashed)
    }

    @Test fun sixteenGlyphsWithLucideNames() {
        assertEquals(16, TimelineGlyph.all.size)
        assertEquals(
            16,
            TimelineGlyph.all
                .map { it.lucideName }
                .toSet()
                .size,
        )
        assertTrue(TimelineGlyph.all.all { it.lucideName == it.lucideName.lowercase() })
    }
}
