package net.chronoflow.core.plan.drawing

import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.plan.generated.TimelinePalette

/** The palette slots the timeline draws with. */
enum class PaletteSlot {
    Background,
    Foreground,
    Card,
    CardForeground,
    Surface2,
    Border,
    MutedForeground,
    CatSleep,
    CatSleepFg,
    CatLight,
    CatLightFg,
    CatModerate,
    CatModerateFg,
    CatDark,
    CatDarkFg,
    CatCaffeine,
    CatCaffeineFg,
    CatTravel,
    CatTravelFg,
}

fun TimelinePalette.color(slot: PaletteSlot): Int =
    when (slot) {
        PaletteSlot.Background -> background
        PaletteSlot.Foreground -> foreground
        PaletteSlot.Card -> card
        PaletteSlot.CardForeground -> cardForeground
        PaletteSlot.Surface2 -> surface2
        PaletteSlot.Border -> border
        PaletteSlot.MutedForeground -> mutedForeground
        PaletteSlot.CatSleep -> catSleep
        PaletteSlot.CatSleepFg -> catSleepFg
        PaletteSlot.CatLight -> catLight
        PaletteSlot.CatLightFg -> catLightFg
        PaletteSlot.CatModerate -> catModerate
        PaletteSlot.CatModerateFg -> catModerateFg
        PaletteSlot.CatDark -> catDark
        PaletteSlot.CatDarkFg -> catDarkFg
        PaletteSlot.CatCaffeine -> catCaffeine
        PaletteSlot.CatCaffeineFg -> catCaffeineFg
        PaletteSlot.CatTravel -> catTravel
        PaletteSlot.CatTravelFg -> catTravelFg
    }

enum class TimelinePattern { DOTS, HATCH }

/** Port of iOS `PlanCategoryStyle` (`PlanAppearance.swift:6-47`); glyphs come from the boards' lucide set (04 OQ-T10). */
class TimelineCategoryStyle private constructor(
    val fill: PaletteSlot,
    val foreground: PaletteSlot,
    val glyph: TimelineGlyph?,
    val pattern: TimelinePattern? = null,
    val dashed: Boolean = false,
) {
    companion object {
        private val sleep = TimelineCategoryStyle(PaletteSlot.CatSleep, PaletteSlot.CatSleepFg, TimelineGlyph.MOON)
        private val nap =
            TimelineCategoryStyle(PaletteSlot.CatSleep, PaletteSlot.CatSleepFg, TimelineGlyph.BED, TimelinePattern.DOTS)
        private val rest =
            TimelineCategoryStyle(
                PaletteSlot.CatSleep,
                PaletteSlot.CatSleepFg,
                TimelineGlyph.ARMCHAIR,
                TimelinePattern.HATCH,
            )
        private val seekLight =
            TimelineCategoryStyle(PaletteSlot.CatLight, PaletteSlot.CatLightFg, TimelineGlyph.SUN)
        private val avoidLight =
            TimelineCategoryStyle(
                PaletteSlot.CatDark,
                PaletteSlot.CatDarkFg,
                TimelineGlyph.GLASSES,
                TimelinePattern.HATCH,
            )
        private val lightOk =
            TimelineCategoryStyle(
                PaletteSlot.CatModerate,
                PaletteSlot.CatModerateFg,
                TimelineGlyph.CLOUD_SUN,
                TimelinePattern.DOTS,
                dashed = true,
            )
        private val lightNeutral =
            TimelineCategoryStyle(PaletteSlot.CatModerate, PaletteSlot.CatModerateFg, TimelineGlyph.LAMP)
        private val caffeineServing =
            TimelineCategoryStyle(
                PaletteSlot.CatCaffeine,
                PaletteSlot.CatCaffeineFg,
                TimelineGlyph.COFFEE,
                TimelinePattern.DOTS,
            )
        private val caffeineRoutine =
            TimelineCategoryStyle(
                PaletteSlot.CatCaffeine,
                PaletteSlot.CatCaffeineFg,
                TimelineGlyph.COFFEE,
                TimelinePattern.HATCH,
            )
        private val caffeineOk =
            TimelineCategoryStyle(PaletteSlot.CatCaffeine, PaletteSlot.CatCaffeineFg, TimelineGlyph.COFFEE)
        private val noCaffeine =
            TimelineCategoryStyle(PaletteSlot.Card, PaletteSlot.Foreground, TimelineGlyph.COFFEE, dashed = true)
        private val flight = TimelineCategoryStyle(PaletteSlot.CatTravel, PaletteSlot.CatTravelFg, TimelineGlyph.PLANE)
        private val layover =
            TimelineCategoryStyle(
                PaletteSlot.CatTravel,
                PaletteSlot.CatTravelFg,
                TimelineGlyph.TIMER,
                TimelinePattern.HATCH,
            )
        private val transfer =
            TimelineCategoryStyle(PaletteSlot.CatTravel, PaletteSlot.CatTravelFg, TimelineGlyph.ARROW_LEFT_RIGHT)
        private val movementBreak =
            TimelineCategoryStyle(PaletteSlot.CatTravel, PaletteSlot.CatTravelFg, TimelineGlyph.FOOTPRINTS)
        private val zoneChange =
            TimelineCategoryStyle(PaletteSlot.CatTravel, PaletteSlot.CatTravelFg, TimelineGlyph.GLOBE)
        private val note = TimelineCategoryStyle(PaletteSlot.Card, PaletteSlot.Foreground, TimelineGlyph.INFO)
        private val unknown = TimelineCategoryStyle(PaletteSlot.Card, PaletteSlot.Foreground, null)

        operator fun invoke(type: BlockType): TimelineCategoryStyle =
            when (type) {
                BlockType.Sleep -> sleep
                BlockType.Nap -> nap
                BlockType.Rest -> rest
                BlockType.SeekLight -> seekLight
                BlockType.AvoidLight -> avoidLight
                BlockType.LightOK -> lightOk
                BlockType.LightNeutral -> lightNeutral
                BlockType.CaffeineServing -> caffeineServing
                BlockType.CaffeineRoutine -> caffeineRoutine
                BlockType.CaffeineOK -> caffeineOk
                BlockType.NoCaffeine -> noCaffeine
                BlockType.Flight -> flight
                BlockType.Layover -> layover
                BlockType.Transfer -> transfer
                BlockType.MovementBreak -> movementBreak
                BlockType.ZoneChange -> zoneChange
                BlockType.Note -> note
                is BlockType.Unknown -> unknown
            }
    }
}
