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

fun checkIdentity(key: CheckKeyView): String = "${key.type.raw}|${key.localDate}|${key.ordinal}"

fun buildCheckState(
    plan: PlanView,
    marks: List<CheckMark>,
): CheckState {
    val actionable = plan.blocks.filter { it.actionable }
    val current = HashMap<String, BlockView>()
    for (block in actionable) block.checkKey?.let { current[checkIdentity(it)] = block }
    val matching = marks.filter { checkIdentity(it.key) in current }
    return CheckState(
        planInputHash = plan.meta.inputHash,
        marks = matching,
        days =
            plan.days.map { day ->
                val dayStart = planDate(day.startUtc)
                val dayEnd = planDate(day.endUtc)

                fun startsOnDay(block: BlockView): Boolean {
                    val start = planDate(block.startUtc)
                    return dayStart != null && dayEnd != null && start != null && dayStart <= start && start < dayEnd
                }
                val dayMarks = matching.filter { mark -> current[checkIdentity(mark.key)]?.let(::startsOnDay) == true }
                DayCheckProgressView(
                    dayId = day.id,
                    done = dayMarks.count { it.status == CheckStatus.Done },
                    skipped = dayMarks.count { it.status == CheckStatus.Skipped },
                    total = actionable.count(::startsOnDay),
                )
            },
    )
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
