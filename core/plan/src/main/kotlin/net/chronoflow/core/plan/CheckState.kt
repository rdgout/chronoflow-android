package net.chronoflow.core.plan

import net.chronoflow.core.model.CheckOffChange
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.BlockView
import net.chronoflow.core.model.contracts.CheckKeyView
import net.chronoflow.core.model.contracts.CheckMark
import net.chronoflow.core.model.contracts.CheckState
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.DayCheckProgressView
import net.chronoflow.core.model.contracts.PlanView
import net.chronoflow.core.model.store.CheckMarkRecord
import java.time.Instant

fun checkIdentity(key: CheckKeyView): String = "${key.type.raw}|${key.localDate}|${key.ordinal}"

fun buildCheckState(
    plan: PlanView,
    marks: List<CheckMark>,
): CheckState {
    val actionableStarts = ArrayList<Instant>()
    val current = HashMap<String, Instant?>()
    for (block in plan.blocks) {
        if (!block.actionable) continue
        val start = planDate(block.startUtc)
        if (start != null) actionableStarts += start
        block.checkKey?.let { current[checkIdentity(it)] = start }
    }
    val matching = ArrayList<CheckMark>()
    val doneStarts = ArrayList<Instant>()
    val skippedStarts = ArrayList<Instant>()
    for (mark in marks) {
        val identity = checkIdentity(mark.key)
        if (!current.containsKey(identity)) continue
        matching += mark
        val start = current[identity] ?: continue
        when (mark.status) {
            CheckStatus.Done -> doneStarts += start
            CheckStatus.Skipped -> skippedStarts += start
            else -> Unit
        }
    }
    actionableStarts.sort()
    doneStarts.sort()
    skippedStarts.sort()
    return CheckState(
        planInputHash = plan.meta.inputHash,
        marks = matching,
        days =
            plan.days.map { day ->
                val dayStart = planDate(day.startUtc)
                val dayEnd = planDate(day.endUtc)
                val bounded = dayStart != null && dayEnd != null
                DayCheckProgressView(
                    dayId = day.id,
                    done = if (bounded) countInRange(doneStarts, dayStart, dayEnd) else 0,
                    skipped = if (bounded) countInRange(skippedStarts, dayStart, dayEnd) else 0,
                    total = if (bounded) countInRange(actionableStarts, dayStart, dayEnd) else 0,
                )
            },
    )
}

private fun countInRange(
    sorted: List<Instant>,
    from: Instant,
    to: Instant,
): Int = (lowerBound(sorted, to) - lowerBound(sorted, from)).coerceAtLeast(0)

private fun lowerBound(
    sorted: List<Instant>,
    value: Instant,
): Int {
    var lo = 0
    var hi = sorted.size
    while (lo < hi) {
        val mid = (lo + hi) ushr 1
        if (sorted[mid] < value) lo = mid + 1 else hi = mid
    }
    return lo
}

fun mergedCheckState(
    plan: PlanView,
    inputHash: String,
    checks: List<CheckMarkRecord>,
    pendingChecks: List<CheckOffChange>,
): CheckState {
    val marks = LinkedHashMap<String, CheckMark>()
    for (row in checks) {
        if (row.inputHash != inputHash) continue
        val key = CheckKeyView(BlockType.of(row.type), row.localDate, row.ordinal)
        marks[checkIdentity(key)] = CheckMark(key, CheckStatus.of(row.status), row.markedAt)
    }
    for (change in pendingChecks) {
        val key = CheckKeyView(change.type, change.localDate, change.ordinal)
        val status = change.status
        if (status != null) marks[checkIdentity(key)] = CheckMark(key, status, "") else marks.remove(checkIdentity(key))
    }
    return buildCheckState(plan, marks.values.sortedBy { checkIdentity(it.key) })
}
