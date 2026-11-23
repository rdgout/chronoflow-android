package net.chronoflow.core.plan

import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.BlockView
import net.chronoflow.core.model.contracts.CheckMark
import net.chronoflow.core.model.contracts.DayCheckProgressView
import net.chronoflow.core.model.contracts.DayView
import net.chronoflow.core.model.contracts.Priority
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.model.contracts.renderableBlocks
import java.time.DateTimeException
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

data class TodayPlan(
    val currentDay: DayView?,
    val currentSteps: List<BlockView>,
    val upcomingSteps: List<BlockView>,
    val marks: List<CheckMark>,
    val dayProgress: DayCheckProgressView?,
    val allCategoriesHidden: Boolean,
    val importantHidden: Boolean,
    val nextStep: BlockView?,
    val minutesUntilNext: Int,
    val beforePlan: Boolean,
    val afterPlan: Boolean,
)

fun todayView(
    detail: TripDetail,
    marks: List<CheckMark>,
    now: Instant,
    hidden: List<BlockType> = emptyList(),
): TodayPlan {
    val plan = withoutOptionalSteps(detail.plan)
    val state = buildCheckState(plan, marks)
    val marked = marks.map { checkIdentity(it.key) }.toSet()
    val excluded = (hidden + listOf(BlockType.LightOK, BlockType.LightNeutral)).toSet()
    val blocks = plan.renderableBlocks.filter { it.type !in excluded }
    val day = plan.days.firstOrNull { containsNow(it.startUtc, it.endUtc, now) }
    val next =
        plan.blocks.firstOrNull { block ->
            block.actionable && block.checkKey?.let { checkIdentity(it) !in marked } == true &&
                planDate(block.endUtc)?.let { it > now } == true
        }
    return TodayPlan(
        currentDay = day,
        currentSteps = blocks.filter { containsNow(it.startUtc, it.endUtc, now) },
        upcomingSteps = blocks.filter { planDate(it.startUtc)?.let { start -> start > now } == true }.take(4),
        marks = state.marks,
        dayProgress = day?.let { current -> state.days.firstOrNull { it.dayId == current.id } },
        allCategoriesHidden = BlockType.known.all { it in hidden },
        importantHidden = detail.plan.blocks.any { it.priority == Priority.Core && it.type in hidden },
        nextStep = next,
        minutesUntilNext =
            next?.let { planDate(it.startUtc) }?.let {
                ceil(Duration.between(now, it).toMillis() / 60_000.0).toInt().coerceAtLeast(0)
            } ?: 0,
        beforePlan =
            planDate(
                plan.days.firstOrNull()?.startUtc ?: detail.trip.departureUtc,
            )?.let { now < it } == true,
        afterPlan = planDate(plan.days.lastOrNull()?.endUtc ?: detail.trip.endUtc)?.let { now >= it } == true,
    )
}

private fun containsNow(
    start: String,
    end: String,
    now: Instant,
): Boolean = planDate(start)?.let { it <= now } == true && planDate(end)?.let { now < it } == true

fun calendarDaysUntil(
    at: String,
    now: Instant,
    zone: String,
): Int {
    val timeZone =
        try {
            ZoneId.of(zone)
        } catch (_: DateTimeException) {
            ZoneOffset.UTC
        }
    val target = planDate(at) ?: return 0
    return ChronoUnit.DAYS
        .between(
            now.atZone(timeZone).toLocalDate(),
            target.atZone(timeZone).toLocalDate(),
        ).toInt()
        .coerceAtLeast(0)
}
