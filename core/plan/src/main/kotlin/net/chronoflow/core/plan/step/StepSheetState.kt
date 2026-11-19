package net.chronoflow.core.plan.step

import net.chronoflow.core.model.contracts.BlockParams
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.plan.CopyFormat
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.plan.blockInterval
import net.chronoflow.core.plan.checkIdentity
import net.chronoflow.core.plan.planDate
import net.chronoflow.core.plan.priorityKey
import net.chronoflow.core.plan.stepLabelKey
import net.chronoflow.core.plan.zoneLabel
import kotlin.math.ceil
import kotlin.math.max

sealed interface StepSheetState {
    data object Gone : StepSheetState

    data class Step(
        val blockId: String,
        val type: BlockType,
        val titleKey: String,
        val priorityKey: String,
        val badge: StepBadge?,
        val interval: String,
        val zone: String,
        val durationLeft: String?,
        val reasons: List<String>,
        val instructions: List<String>,
        val actions: StepActions,
    ) : StepSheetState
}

enum class StepBadge { Done, Skipped, Now }

enum class StepActions { None, Open, Marked }

fun stepSheetState(
    detail: TripDetail?,
    blockId: String,
    nowMs: Long,
    formatter: CopyLineFormatter,
    language: Language,
    timeFormat: TimeFormat,
): StepSheetState {
    val block = detail?.plan?.blocks?.firstOrNull { it.id == blockId } ?: return StepSheetState.Gone
    val key = if (block.actionable) block.checkKey else null
    val mark = key?.let { k -> detail.checkState.marks.firstOrNull { checkIdentity(it.key) == checkIdentity(k) } }
    val start = planDate(block.startUtc)?.toEpochMilli()
    val end = planDate(block.endUtc)?.toEpochMilli()
    val running = start != null && end != null && start <= nowMs && nowMs < end
    val options = CopyFormat(language, timeFormat, block.displayTz, true, detail.trip)
    val params = block.params
    val instructions =
        if (params is BlockParams.Note) {
            params.value.note.lines
        } else {
            listOf(block.text) +
                listOfNotNull(block.secondaryText)
        }
    return StepSheetState.Step(
        block.id,
        block.type,
        stepLabelKey(block.type) ?: "Note",
        priorityKey(block.priority),
        when {
            mark != null -> if (mark.status == CheckStatus.Done) StepBadge.Done else StepBadge.Skipped
            running -> StepBadge.Now
            else -> null
        },
        if (block.type ==
            BlockType.ZoneChange
        ) {
            formatter.formatInstant(block.startUtc, options)
        } else {
            formatter.blockInterval(block, options, separator = " → ")
        },
        zoneLabel(block.displayTz, detail.trip),
        if (running) {
            formatter.formatMinutes(
                max(0.0, ceil((end - nowMs) / 60_000.0)),
                language,
                duration = true,
            )
        } else {
            null
        },
        detail.timeline.blockReasons
            .firstOrNull {
                it.blockId == block.id
            }?.lines
            .orEmpty()
            .map { formatter.format(it, options) },
        instructions.map { formatter.format(it, options) },
        when {
            key == null -> StepActions.None
            mark != null -> StepActions.Marked
            else -> StepActions.Open
        },
    )
}
