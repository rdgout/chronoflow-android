package net.chronoflow.core.plan.drawing

import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.Priority
import net.chronoflow.core.plan.generated.TimelinePalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineColorsTest {
    private val light = TimelinePalette.Light

    @Test fun fullAlphaReturnsTheColorUnchanged() {
        assertEquals(light.catSleep, TimelineColors.precomposite(light.catSleep, 1.0, light.surface2))
    }

    @Test fun catSleepAtMarkedAlphaOverSurface2MatchesTheHandComputedValue() {
        fun channel(shift: Int): Int {
            val c = (light.catSleep shr shift) and 0xFF
            val u = (light.surface2 shr shift) and 0xFF
            return Math.round(0.6 * c + 0.4 * u).toInt()
        }
        val expected = (0xFF shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
        assertEquals(expected, TimelineColors.precomposite(light.catSleep, 0.6, light.surface2))
    }

    @Test fun withAlphaWritesTheRoundedAlphaByte() {
        assertEquals(41, TimelineColors.withAlpha(light.catSleep, 0.16) ushr 24)
        assertEquals(light.catSleep and 0xFFFFFF, TimelineColors.withAlpha(light.catSleep, 0.5) and 0xFFFFFF)
    }

    @Test fun hatchTableHasTwelveDeterministicEntries() {
        val a = TimelineColors.hatchColorTable(light)
        assertEquals(12, a.size)
        assertTrue(a.contentEquals(TimelineColors.hatchColorTable(light)))
        assertTrue(a.all { it ushr 24 == 31 })
        assertEquals(0, TimelineColors.hatchIndex(PaletteSlot.CatSleepFg, 1.0))
        assertEquals(11, TimelineColors.hatchIndex(PaletteSlot.CatTravelFg, 0.6))
    }

    @Test fun groupAlphaFollowsStatusThenPriority() {
        assertEquals(0.6, TimelineColors.groupAlpha(CheckStatus.Done, Priority.Core), 0.0)
        assertEquals(0.75, TimelineColors.groupAlpha(null, Priority.Optional), 0.0)
        assertEquals(0.6, TimelineColors.groupAlpha(CheckStatus.Skipped, Priority.Optional), 0.0)
        assertEquals(0.6, TimelineColors.groupAlpha(CheckStatus.Unknown("x"), Priority.Recommended), 0.0)
        assertEquals(1.0, TimelineColors.groupAlpha(null, Priority.Recommended), 0.0)
    }
}
