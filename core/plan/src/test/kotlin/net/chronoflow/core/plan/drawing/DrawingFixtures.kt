package net.chronoflow.core.plan.drawing

import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.Lane
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.Priority
import net.chronoflow.core.plan.DayLayout
import net.chronoflow.core.plan.PlacedBlock
import net.chronoflow.core.plan.PreparedDay
import net.chronoflow.core.plan.PreparedPlan
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.plan.generated.TimelinePalette
import net.chronoflow.core.plan.layoutPlan
import net.chronoflow.core.plan.prepare
import net.chronoflow.core.plan.withoutOptionalSteps
import net.chronoflow.core.testing.FakeTextMetrics
import net.chronoflow.core.testing.LargeTimelineFixtures

class PreparedFixture(
    val plan: PreparedPlan,
    val layouts: Map<String, DayLayout>,
)

object DrawingFixtures {
    private val cache = HashMap<String, PreparedFixture>()

    fun prepared(
        name: String,
        language: Language = Language.En,
    ): PreparedFixture =
        synchronized(cache) {
            cache.getOrPut("$name/${language.raw}") {
                val detail = LargeTimelineFixtures.fixture(name).detail
                val layouts = layoutPlan(withoutOptionalSteps(detail.plan), detail.timeline)
                PreparedFixture(prepare(detail, language, layouts = layouts), layouts)
            }
        }

    /** A window width in dp: content = window - 2 x gutter, compact below the label breakpoint. */
    fun key(
        day: PreparedDay,
        revision: String,
        window: Int,
        density: Float = 1f,
        dark: Boolean = false,
        language: Language = Language.En,
        textScale: Float = 1f,
        compact: Boolean = window < PlanMetrics.timelineLabelBreakpoint,
    ) = DayDrawingKey(
        day.id,
        revision,
        Math.round((window - 2 * PlanMetrics.gutter).toFloat() * density),
        density,
        textScale,
        dark,
        compact,
        language,
    )

    fun palette(dark: Boolean): TimelinePalette = if (dark) TimelinePalette.Dark else TimelinePalette.Light

    fun build(
        day: PreparedDay,
        layout: DayLayout,
        key: DayDrawingKey,
        metrics: TextMetrics = FakeTextMetrics(),
        precomposite: Boolean = true,
    ): DayDrawing = DayDrawingBuilder.build(day, layout, key, palette(key.dark), metrics, precomposite)

    /** The day of the iOS alpha tests: one tile forced into the first sleep-lane slot, nothing else. */
    class SingleTile(
        val day: PreparedDay,
        val layout: DayLayout,
        val key: DayDrawingKey,
    ) {
        fun build(
            metrics: TextMetrics = FakeTextMetrics(),
            precomposite: Boolean = true,
        ): DayDrawing = build(day, layout, key, metrics, precomposite)
    }

    /**
     * One tile of the given dp size in the first column of the sleep lane. The lane width follows from the size and
     * [cols]; `minWidth` is zeroed so the available width alone sets the geometry.
     */
    fun singleTile(
        type: BlockType = BlockType.Sleep,
        tileWidth: Double = 120.0,
        tileHeight: Double = 80.0,
        cols: Int = 1,
        tiny: Boolean = false,
        compact: Boolean = false,
        continuesBefore: Boolean = false,
        continuesAfter: Boolean = false,
        priority: Priority = Priority.Recommended,
        status: CheckStatus? = null,
        top: Double = 10.0,
        density: Float = 1f,
        dark: Boolean = false,
        textScale: Float = 1f,
        title: String = "Title",
        interval: String = "10:00 – 11:00",
        reason: String? = null,
        priorityLabel: String = "Core",
    ): SingleTile {
        val fixture = prepared("S3")
        val day = fixture.plan.days.first()
        val layout = fixture.layouts.getValue(day.id)
        val source = layout.placed.getValue(Lane.Sleep).first()
        val block = source.block.copy(type = type, priority = priority)
        val placed =
            source.copy(
                block = block,
                top = top,
                height = tileHeight,
                exactTop = top,
                tiny = tiny,
                col = 0,
                cols = cols,
                continuesBefore = continuesBefore,
                continuesAfter = continuesAfter,
            )
        val columnWidth = tileWidth + PlanMetrics.tileInset * 2
        val width = PlanMetrics.gutterWidth + 4 * (columnWidth * cols + PlanMetrics.laneGap)
        val singleLayout =
            layout.copy(
                height = top + tileHeight + 10.0,
                minWidth = 0.0,
                placed =
                    mapOf(
                        Lane.Sleep to listOf<PlacedBlock>(placed),
                        Lane.Light to emptyList(),
                        Lane.Caffeine to emptyList(),
                        Lane.Travel to emptyList(),
                    ),
                markers = emptyList(),
                changes = emptyList(),
                dividers = emptyList(),
                ticks = emptyList(),
            )
        val prepared =
            day.blocks.first { it.id == source.block.id }.copy(
                block = block,
                title = title,
                interval = interval,
                priority = priorityLabel,
                reason = reason,
                status = status,
                continuesBefore = continuesBefore,
                continuesAfter = continuesAfter,
            )
        val singleDay = day.copy(blocks = listOf(prepared), ticks = emptyList(), dividerLabels = emptyList())
        val key =
            DayDrawingKey(
                singleDay.id,
                "single",
                Math.round(width * density).toInt(),
                density,
                textScale,
                dark,
                compact,
                Language.En,
            )
        return SingleTile(singleDay, singleLayout, key)
    }
}
