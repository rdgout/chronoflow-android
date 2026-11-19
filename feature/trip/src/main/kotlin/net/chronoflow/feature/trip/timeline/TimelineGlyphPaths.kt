package net.chronoflow.feature.trip.timeline

import android.graphics.Path
import androidx.core.graphics.PathParser
import net.chronoflow.core.designsystem.icon.LucideGlyph
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.plan.drawing.TimelineGlyph

/** Lucide glyph paths in the 24-unit space, parsed once per process and indexed by [TimelineGlyph.ordinal]. */
internal object TimelineGlyphPaths {
    private class Parsed(
        val paths: Array<Array<Path>>,
        val filled: Array<BooleanArray>,
        val thread: Thread,
    )

    @Volatile
    private var parsed: Parsed? = null

    val paths: Array<Array<Path>> get() = parsedOrParse().paths

    val filled: Array<BooleanArray> get() = parsedOrParse().filled

    val isParsed: Boolean get() = parsed != null

    internal val parsedOnThread: Thread? get() = parsed?.thread

    /** Parses every glyph path now; [TimelineTypefaces.load] calls it on an IO thread so the first replay never parses. */
    fun warm() {
        parsedOrParse()
    }

    internal fun resetForTest() {
        parsed = null
    }

    private fun parsedOrParse(): Parsed =
        parsed ?: synchronized(this) {
            parsed ?: Parsed(
                Array(TimelineGlyph.all.size) { index -> parse(source(TimelineGlyph.all[index])) },
                Array(TimelineGlyph.all.size) { index ->
                    val glyph = source(TimelineGlyph.all[index])
                    BooleanArray(glyph.paths.size) { glyph.paths[it].filled }
                },
                Thread.currentThread(),
            ).also { parsed = it }
        }

    private fun parse(glyph: LucideGlyph): Array<Path> =
        Array(glyph.paths.size) { PathParser.createPathFromPathData(glyph.paths[it].d) }

    private fun source(glyph: TimelineGlyph): LucideGlyph =
        when (glyph) {
            TimelineGlyph.MOON -> LucideGlyphs.Moon
            TimelineGlyph.BED -> LucideGlyphs.Bed
            TimelineGlyph.ARMCHAIR -> LucideGlyphs.Armchair
            TimelineGlyph.SUN -> LucideGlyphs.Sun
            TimelineGlyph.GLASSES -> LucideGlyphs.Glasses
            TimelineGlyph.CLOUD_SUN -> LucideGlyphs.CloudSun
            TimelineGlyph.LAMP -> LucideGlyphs.Lamp
            TimelineGlyph.COFFEE -> LucideGlyphs.Coffee
            TimelineGlyph.PLANE -> LucideGlyphs.Plane
            TimelineGlyph.TIMER -> LucideGlyphs.Timer
            TimelineGlyph.ARROW_LEFT_RIGHT -> LucideGlyphs.ArrowLeftRight
            TimelineGlyph.FOOTPRINTS -> LucideGlyphs.Footprints
            TimelineGlyph.GLOBE -> LucideGlyphs.Globe
            TimelineGlyph.INFO -> LucideGlyphs.Info
            TimelineGlyph.CHECK -> LucideGlyphs.Check
            TimelineGlyph.SKIP_FORWARD -> LucideGlyphs.SkipForward
        }
}
