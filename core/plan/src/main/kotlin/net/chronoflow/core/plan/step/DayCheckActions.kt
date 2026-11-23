package net.chronoflow.core.plan.step

import net.chronoflow.core.plan.PreparedBlock
import net.chronoflow.core.plan.PreparedDay

data class DayCheckActions(
    val byBlock: Map<String, StepActions>,
)

fun checkActions(day: PreparedDay): DayCheckActions = day.checkActions

internal fun checkActions(blocks: List<PreparedBlock>): DayCheckActions =
    DayCheckActions(
        blocks.filter { it.block.actionable && it.block.checkKey != null }.associate {
            it.id to
                if (it.status == null) StepActions.Open else StepActions.Marked
        },
    )
