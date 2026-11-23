package net.chronoflow.core.plan

import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.BlockView
import net.chronoflow.core.model.contracts.CopyLine
import net.chronoflow.core.model.contracts.DayView
import net.chronoflow.core.model.contracts.DayZoneView
import net.chronoflow.core.model.contracts.Lane
import net.chronoflow.core.model.contracts.PlanView
import net.chronoflow.core.model.contracts.TickView
import net.chronoflow.core.model.contracts.TimelinePresentationView
import net.chronoflow.core.model.contracts.isSupported
import net.chronoflow.core.plan.generated.PlanMetrics
import java.time.Instant

data class BlockPresentation(
    val reasons: List<CopyLine>,
    val warningIndexes: List<Int>,
)

data class PlacedBlock(
    val block: BlockView,
    val top: Double,
    val height: Double,
    val exactTop: Double,
    val tiny: Boolean,
    val col: Int,
    val cols: Int,
    val continuesBefore: Boolean,
    val continuesAfter: Boolean,
    val presentation: BlockPresentation,
)

data class PlacedMarker(
    val block: BlockView,
    val exactTop: Double,
    val top: Double,
    val presentation: BlockPresentation,
)

data class ZoneChangePosition(
    val block: BlockView,
    val top: Double,
)

data class ZoneDivider(
    val before: DayZoneView,
    val after: DayZoneView,
    val top: Double,
)

data class TickPosition(
    val tick: TickView,
    val top: Double,
    val visible: Boolean,
)

data class TimelineIndex(
    val ticks: Map<String, List<TickView>>,
    val blockReasons: Map<String, List<CopyLine>>,
    val transferWarnings: Map<String, List<Int>>,
)

data class DayLayout(
    val day: DayView,
    val height: Double,
    val minWidth: Double,
    val placed: Map<Lane, List<PlacedBlock>>,
    val markers: List<PlacedMarker>,
    val changes: List<ZoneChangePosition>,
    val dividers: List<ZoneDivider>,
    val ticks: List<TickPosition>,
)

private val laneOrder: List<Lane> = listOf(Lane.Sleep, Lane.Light, Lane.Caffeine, Lane.Travel)

val timelineLanes: List<Lane> get() = laneOrder

fun indexTimeline(timeline: TimelinePresentationView): TimelineIndex =
    TimelineIndex(
        ticks = timeline.days.associate { it.dayId to it.ticks },
        blockReasons = timeline.blockReasons.associate { it.blockId to it.lines },
        transferWarnings = timeline.transferWarnings.associate { it.blockId to it.warningIndexes },
    )

private fun secondsBetween(
    start: Instant,
    at: Instant,
): Double = (at.epochSecond - start.epochSecond) + (at.nano - start.nano) / 1e9

private fun offsetFrom(
    start: Instant,
    at: Instant,
): Double = secondsBetween(start, at) / 60 * PlanMetrics.pointsPerMinute

fun nowLineOffset(
    day: DayView,
    now: Instant,
): Double? {
    val start = planDate(day.startUtc) ?: return null
    val end = planDate(day.endUtc) ?: return null
    return if (start <= now && now < end) offsetFrom(start, now) else null
}

fun zoneChangePositions(
    day: DayView,
    blocks: List<BlockView>,
): List<ZoneChangePosition> {
    val start = planDate(day.startUtc) ?: return emptyList()
    val end = planDate(day.endUtc) ?: return emptyList()
    val found = ArrayList<Pair<Instant, BlockView>>()
    for (block in blocks) {
        if (block.type != BlockType.ZoneChange) continue
        val at = planDate(block.startUtc) ?: continue
        if (start <= at && at < end) found.add(at to block)
    }
    found.sortWith { a, b -> a.first.compareTo(b.first) }
    return found.map { (at, block) -> ZoneChangePosition(block, offsetFrom(start, at)) }
}

fun tickPositions(
    day: DayView,
    ticks: List<TickView>,
): List<TickPosition> {
    val start = planDate(day.startUtc) ?: return emptyList()
    val end = planDate(day.endUtc) ?: return emptyList()
    val inside = ArrayList<TickView>(ticks.size)
    val positions = ArrayList<Double>(ticks.size)
    for (tick in ticks) {
        val at = planDate(tick.atUtc) ?: continue
        if (start <= at && at < end) {
            inside.add(tick)
            positions.add(offsetFrom(start, at))
        }
    }
    val mandatory =
        BooleanArray(inside.size) {
            it == 0 || inside[it].tz != inside[it - 1].tz || inside[it].offsetMin != inside[it - 1].offsetMin
        }
    val visible = ArrayList<Int>()
    for (index in inside.indices) {
        if (mandatory[index]) {
            while (visible.isNotEmpty()) {
                val last = visible.last()
                if (positions[index] - positions[last] < PlanMetrics.minTickSpacing && !mandatory[last]) {
                    visible.removeAt(visible.size - 1)
                } else {
                    break
                }
            }
            visible.add(index)
        } else if (visible.isNotEmpty() &&
            positions[index] - positions[visible.last()] >= PlanMetrics.minTickSpacing
        ) {
            visible.add(index)
        }
    }
    val shown = BooleanArray(inside.size)
    for (index in visible) shown[index] = true
    return inside.indices.map { TickPosition(inside[it], positions[it], shown[it]) }
}

private val packOrder =
    Comparator<PlacedBlock> { a, b -> if (a.top == b.top) a.exactTop.compareTo(b.exactTop) else a.top.compareTo(b.top) }

fun packLane(items: List<PlacedBlock>): List<PlacedBlock> {
    val sorted = items.sortedWith(packOrder)
    val cols = IntArray(sorted.size)
    val col = IntArray(sorted.size)
    var groupStart = 0
    val columnEnds = ArrayList<Double>()
    var groupEnd = Double.NEGATIVE_INFINITY

    fun flush(until: Int) {
        for (index in groupStart until until) cols[index] = columnEnds.size
        groupStart = until
        columnEnds.clear()
    }
    for (index in sorted.indices) {
        val item = sorted[index]
        if (item.top >= groupEnd) flush(index)
        var column = columnEnds.size
        for (candidate in columnEnds.indices) {
            if (columnEnds[candidate] <= item.top) {
                column = candidate
                break
            }
        }
        col[index] = column
        val end = item.top + item.height
        if (column == columnEnds.size) columnEnds.add(end) else columnEnds[column] = end
        groupEnd = maxOf(groupEnd, end)
    }
    flush(sorted.size)
    return List(sorted.size) { sorted[it].copy(col = col[it], cols = cols[it]) }
}

private fun emptyLayout(day: DayView): DayLayout {
    val placed = LinkedHashMap<Lane, List<PlacedBlock>>()
    for (lane in laneOrder) placed[lane] = emptyList()
    return DayLayout(
        day,
        0.0,
        minWidthOf(
            IntArray(laneOrder.size) {
                1
            },
        ),
        placed,
        emptyList(),
        emptyList(),
        emptyList(),
        emptyList(),
    )
}

private fun minWidthOf(columns: IntArray): Double {
    var width = PlanMetrics.gutterWidth + laneOrder.size * PlanMetrics.laneGap
    for (count in columns) width += maxOf(PlanMetrics.minLaneWidth, count * PlanMetrics.minColumnWidth)
    return width
}

fun layoutDay(
    day: DayView,
    blocks: List<BlockView>,
    presentation: TimelineIndex,
    hidden: Set<BlockType> = emptySet(),
): DayLayout {
    val start = planDate(day.startUtc) ?: return emptyLayout(day)
    val end = planDate(day.endUtc) ?: return emptyLayout(day)
    val height = offsetFrom(start, end)
    val lanes = Array(laneOrder.size) { ArrayList<PlacedBlock>() }
    val markers = ArrayList<PlacedMarker>()
    for (block in blocks) {
        if (!block.isSupported || isOptionalStep(block) || block.type in hidden || block.type == BlockType.ZoneChange) {
            continue
        }
        val a = planDate(block.startUtc) ?: continue
        val b = planDate(block.endUtc) ?: continue
        if (a >= end || (b <= start && !(a == b && a >= start))) continue
        val clippedStart = if (a > start) a else start
        val exactTop = offsetFrom(start, clippedStart)
        val details =
            BlockPresentation(
                presentation.blockReasons[block.id].orEmpty(),
                presentation.transferWarnings[block.id].orEmpty(),
            )
        if (block.lane == Lane.Info) {
            markers.add(PlacedMarker(block, exactTop, exactTop + PlanMetrics.gutterMarkerSize / 2, details))
            continue
        }
        val lane = laneOrder.indexOf(block.lane)
        val tileHeight =
            minOf(height, maxOf(PlanMetrics.minTileHeight, offsetFrom(clippedStart, if (b < end) b else end)))
        lanes[lane].add(
            PlacedBlock(
                block = block,
                top = minOf(exactTop, height - tileHeight),
                height = tileHeight,
                exactTop = exactTop,
                tiny = tileHeight <= PlanMetrics.tinyTileHeight,
                col = 0,
                cols = 1,
                continuesBefore = a < start,
                continuesAfter = b > end,
                presentation = details,
            ),
        )
    }
    val placed = LinkedHashMap<Lane, List<PlacedBlock>>()
    val columns = IntArray(laneOrder.size)
    for ((index, lane) in laneOrder.withIndex()) {
        val packed = packLane(lanes[index])
        placed[lane] = packed
        columns[index] = maxOf(1, packed.maxOfOrNull { it.cols } ?: 1)
    }
    val ordered = markers.sortedBy { it.exactTop }
    val spaced = ArrayList<PlacedMarker>(ordered.size)
    for ((index, marker) in ordered.withIndex()) {
        spaced.add(
            if (index ==
                0
            ) {
                marker
            } else {
                marker.copy(top = maxOf(marker.top, spaced[index - 1].top + PlanMetrics.gutterMarkerSize))
            },
        )
    }
    val dividers = ArrayList<ZoneDivider>()
    for (index in 1 until day.zones.size) {
        val at = planDate(day.zones[index].startUtc) ?: continue
        if (start <= at && at < end) {
            dividers.add(ZoneDivider(day.zones[index - 1], day.zones[index], offsetFrom(start, at)))
        }
    }
    return DayLayout(
        day = day,
        height = height,
        minWidth = minWidthOf(columns),
        placed = placed,
        markers = spaced,
        changes =
            if (BlockType.ZoneChange in hidden) {
                emptyList()
            } else {
                zoneChangePositions(day, blocks.filter { it.isSupported })
            },
        dividers = dividers,
        ticks = tickPositions(day, presentation.ticks[day.id].orEmpty()),
    )
}

fun layoutPlan(
    plan: PlanView,
    timeline: TimelinePresentationView,
    hidden: List<BlockType> = emptyList(),
): Map<String, DayLayout> {
    val presentation = indexTimeline(timeline)
    val excluded = hidden.toSet()
    val blocks = timelineBlocksByDay(plan)
    val result = LinkedHashMap<String, DayLayout>(plan.days.size * 2)
    for (day in plan.days) result[day.id] = layoutDay(day, blocks[day.id].orEmpty(), presentation, excluded)
    return result
}

private class TimedBlock(
    val index: Int,
    val block: BlockView,
    val start: Instant,
    val end: Instant,
)

fun timelineBlocksByDay(plan: PlanView): Map<String, List<BlockView>> {
    val ordered = ArrayList<TimedBlock>(plan.blocks.size)
    for ((index, block) in plan.blocks.withIndex()) {
        val start = planDate(block.startUtc) ?: continue
        val end = planDate(block.endUtc) ?: continue
        ordered.add(TimedBlock(index, block, start, end))
    }
    ordered.sortWith { a, b -> if (a.start == b.start) a.index.compareTo(b.index) else a.start.compareTo(b.start) }
    val result = HashMap<String, List<BlockView>>(plan.days.size * 2)
    val dated = ArrayList<Triple<DayView, Instant, Instant>>(plan.days.size)
    for (day in plan.days) {
        val start = planDate(day.startUtc)
        val end = planDate(day.endUtc)
        if (start == null || end == null) result[day.id] = emptyList() else dated.add(Triple(day, start, end))
    }
    dated.sortWith { a, b -> a.second.compareTo(b.second) }
    var cursor = 0
    val active = ArrayList<TimedBlock>()
    for ((day, start, end) in dated) {
        while (cursor < ordered.size && ordered[cursor].start < end) {
            active.add(ordered[cursor])
            cursor++
        }
        active.removeAll { it.end <= start && !(it.start == it.end && it.start >= start) }
        result[day.id] = active.filter { it.start < end }.sortedBy { it.index }.map { it.block }
    }
    return result
}
