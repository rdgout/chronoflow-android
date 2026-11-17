package net.chronoflow.feature.trip.timeline

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.Lane
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.Priority
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.plan.CopyCatalogProvider
import net.chronoflow.core.plan.DayLayout
import net.chronoflow.core.plan.PlacedBlock
import net.chronoflow.core.plan.PlanDecoder
import net.chronoflow.core.plan.PlanPreparer
import net.chronoflow.core.plan.PreparedDay
import net.chronoflow.core.plan.PreparedPlan
import net.chronoflow.core.plan.drawing.DayDrawing
import net.chronoflow.core.plan.drawing.DayDrawingBuilder
import net.chronoflow.core.plan.drawing.DayDrawingKey
import net.chronoflow.core.plan.drawing.TextMetrics
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.plan.generated.TimelinePalette
import net.chronoflow.core.testing.LargeTimelineFixtures
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

class PreparedFixture(
    val plan: PreparedPlan,
    val layouts: Map<String, DayLayout>,
)

/** Prepared fixtures through the real preparer, once per fixture and language per test JVM. */
object TimelineTestPlans {
    const val DENSITY = 2.625f
    private val cache = HashMap<String, PreparedFixture>()

    fun prepared(
        name: String,
        language: Language = Language.En,
    ): PreparedFixture =
        synchronized(cache) {
            cache.getOrPut("$name/${language.raw}") {
                var result: PreparedFixture? = null
                runTest(timeout = 60.seconds) {
                    val detail = LargeTimelineFixtures.fixture(name).detail
                    val preparer =
                        PlanPreparer(
                            PlanDecoder(Dispatchers.Default),
                            CopyCatalogProvider(Dispatchers.IO),
                            Dispatchers.Default,
                        )
                    val layouts = preparer.layouts(detail, emptyList())
                    val plan = preparer.prepare(detail, emptyList(), language, TimeFormat.Value24, emptyList())
                    result = PreparedFixture(plan, layouts)
                }
                checkNotNull(result)
            }
        }

    fun palette(dark: Boolean): TimelinePalette = if (dark) TimelinePalette.Dark else TimelinePalette.Light

    /** A window width in dp: content = window - 2 x gutter, compact below the label breakpoint. */
    fun key(
        day: PreparedDay,
        revision: String,
        window: Int,
        density: Float = DENSITY,
        dark: Boolean = false,
        language: Language = Language.En,
        fontScale: Float = 1f,
        compact: Boolean = window < PlanMetrics.timelineLabelBreakpoint,
    ) = DayDrawingKey(
        day.id,
        revision,
        ((window - 2 * PlanMetrics.gutter) * density).roundToInt(),
        density,
        timelineTextScale(Density(density, fontScale)),
        dark,
        compact,
        language,
    )

    fun build(
        day: PreparedDay,
        layout: DayLayout,
        key: DayDrawingKey,
        metrics: TextMetrics,
        precomposite: Boolean = true,
    ): DayDrawing = DayDrawingBuilder.build(day, layout, key, palette(key.dark), metrics, precomposite)

    fun render(
        drawing: DayDrawing,
        resources: TimelineResources,
        bitmap: Bitmap,
        background: Int,
    ) {
        bitmap.eraseColor(background)
        replayDayDrawing(Canvas(bitmap), drawing, resources)
    }

    class SingleTile(
        val day: PreparedDay,
        val layout: DayLayout,
        val key: DayDrawingKey,
        val placed: PlacedBlock,
    )

    /**
     * One tile in the first column(s) of the sleep lane. The block is the first sleep-lane block of the first S3 day
     * (or the first of type sleep when [firstOfTypeSleep] is set); the width is set through the key.
     */
    fun singleTile(
        type: BlockType? = null,
        priority: Priority,
        status: CheckStatus? = null,
        top: Double,
        height: Double,
        cols: Int,
        window: Int,
        density: Float = DENSITY,
        dark: Boolean = false,
        compact: Boolean = false,
        layoutHeight: Double = top + height + 10,
        firstOfTypeSleep: Boolean = false,
    ): SingleTile {
        val fixture = prepared("S3")
        val day =
            if (firstOfTypeSleep) {
                fixture.plan.days.first { d ->
                    fixture.layouts
                        .getValue(d.id)
                        .placed
                        .getValue(Lane.Sleep)
                        .any { it.block.type == BlockType.Sleep }
                }
            } else {
                fixture.plan.days.first()
            }
        val layout = fixture.layouts.getValue(day.id)
        val source =
            layout.placed.getValue(Lane.Sleep).first { !firstOfTypeSleep || it.block.type == BlockType.Sleep }
        val block = source.block.copy(type = type ?: source.block.type, priority = priority)
        val placed =
            source.copy(
                block = block,
                top = top,
                height = height,
                exactTop = top,
                tiny = false,
                col = 0,
                cols = cols,
                continuesBefore = false,
                continuesAfter = false,
            )
        val prepared =
            day.blocks.first { it.id == source.block.id }.copy(
                block = block,
                status = status,
                continuesBefore = false,
                continuesAfter = false,
            )
        val singleLayout =
            layout.copy(
                height = layoutHeight,
                minWidth = 0.0,
                placed =
                    mapOf(
                        Lane.Sleep to listOf(placed),
                        Lane.Light to emptyList(),
                        Lane.Caffeine to emptyList(),
                        Lane.Travel to emptyList(),
                    ),
                markers = emptyList(),
                changes = emptyList(),
                dividers = emptyList(),
                ticks = emptyList(),
            )
        val singleDay = day.copy(blocks = listOf(prepared), ticks = emptyList(), dividerLabels = emptyList())
        val key = key(singleDay, "single", window, density, dark, compact = compact)
        return SingleTile(singleDay, singleLayout, key, placed)
    }
}
