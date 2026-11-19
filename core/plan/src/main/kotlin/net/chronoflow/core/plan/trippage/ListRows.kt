package net.chronoflow.core.plan.trippage

import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.Priority
import net.chronoflow.core.plan.PageCopy
import net.chronoflow.core.plan.PreparedBlock
import net.chronoflow.core.plan.PreparedPlan
import net.chronoflow.core.plan.planDate

data class ListRow(
    val key: String,
    val dayId: String,
    val blockId: String,
    val zone: Boolean,
    val type: BlockType,
    val title: String,
    val interval: String,
    val intervalNow: String,
    val label: String,
    val labelNow: String,
    val priorityTag: String?,
    val status: CheckStatus?,
    val optional: Boolean,
    val startMs: Long,
    val endMs: Long,
    val zoneInstruction: String?,
    val zoneComparison: String?,
)

data class DayListRows(
    val dayId: String,
    val rows: List<ListRow>,
)

object ListRows {
    fun build(
        plan: PreparedPlan,
        copy: PageCopy,
    ): List<DayListRows> =
        plan.days.map { day ->
            DayListRows(day.id, day.blocks.map { row(day.id, it, copy) })
        }

    private fun row(
        dayId: String,
        prepared: PreparedBlock,
        copy: PageCopy,
    ): ListRow {
        val block = prepared.block
        val zone = block.type == BlockType.ZoneChange
        val priority =
            prepared.priority.takeIf {
                block.priority == Priority.Core || block.priority == Priority.Optional
            }
        val status = prepared.status?.let { copy(if (it == CheckStatus.Done) "Marked done" else "Skipped") }
        val now = if (zone) prepared.interval else copy("%@ · now", prepared.interval)

        fun label(interval: String) =
            if (zone) {
                listOfNotNull(prepared.title, prepared.zoneInstruction, prepared.zoneComparison).joinToString(". ")
            } else {
                listOfNotNull(prepared.title, interval, priority, status).joinToString(", ")
            }
        val start = planDate(block.startUtc)
        val end = planDate(block.endUtc)
        val valid = start != null && end != null
        return ListRow(
            key = "${if (zone) "z" else "r"}:$dayId:${block.id}",
            dayId = dayId,
            blockId = block.id,
            zone = zone,
            type = block.type,
            title = prepared.title,
            interval = prepared.interval,
            intervalNow = now,
            label = label(prepared.interval),
            labelNow = label(now),
            priorityTag = priority,
            status = prepared.status,
            optional = block.priority == Priority.Optional,
            startMs = if (valid) start.toEpochMilli() else Long.MAX_VALUE,
            endMs = if (valid) end.toEpochMilli() else Long.MIN_VALUE,
            zoneInstruction = prepared.zoneInstruction,
            zoneComparison = prepared.zoneComparison,
        )
    }
}
