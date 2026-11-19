package net.chronoflow.feature.trip.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.effect.cfClickable
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.designsystem.theme.tnum
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.plan.step.StepActions
import net.chronoflow.core.plan.trippage.ListRow
import net.chronoflow.feature.trip.step.checkAccessibilityActions
import net.chronoflow.feature.trip.step.rememberCheckActionLabels
import net.chronoflow.feature.trip.timeline.DayClock
import net.chronoflow.feature.trip.timeline.LocalTimelineProbe
import net.chronoflow.feature.trip.timeline.TimelineProbe

@Composable
fun DayListRow(
    row: ListRow,
    clock: DayClock?,
    onBlock: (String) -> Unit,
    modifier: Modifier = Modifier,
    actions: StepActions = StepActions.None,
    onCheck: (String, CheckStatus?) -> Unit = { _, _ -> },
    selected: () -> Boolean = { false },
) {
    val labels = rememberCheckActionLabels()
    val checkActions =
        remember(row.blockId, actions, labels, onCheck) {
            checkAccessibilityActions(actions, row.blockId, labels, onCheck)
        }
    val running by remember(row, clock) {
        derivedStateOf { clock?.value?.value?.let { row.startMs <= it.nowMs && it.nowMs < row.endMs } == true }
    }
    val colors = CfTheme.colors
    val shape = RoundedCornerShape(PlanMetrics.rowRadius.dp)
    val probe = LocalTimelineProbe.current
    SideEffect { probe?.composed(TimelineProbe.Kind.LIST_ROW, row.key) }
    Layout(
        content = {
            TripPageCategorySwatch(row.type)
            Column(verticalArrangement = Arrangement.spacedBy(PlanMetrics.smallGap.dp)) {
                Text(
                    row.title,
                    Modifier.testTag("trip.block.${row.blockId}.title"),
                    style = CfTheme.type.authStrong,
                    color = colors.cardForeground,
                )
                Text(
                    if (running) row.intervalNow else row.interval,
                    Modifier.testTag("trip.block.${row.blockId}.interval"),
                    style = CfTheme.type.planListLine.tnum(),
                    color = colors.mutedForeground,
                )
            }
            RowTrailing(row)
        },
        modifier =
            modifier
                .fillMaxWidth()
                .alpha(if (row.status != null) PlanMetrics.markedRowOpacity.toFloat() else 1f)
                .background(colors.card, shape)
                .tripOutline(colors.border, PlanMetrics.rowRadius.dp, row.optional, selected, colors.dawn)
                .cfClickable(onClick = { onBlock(row.blockId) }, focusShape = shape)
                .testTag("trip.block.${row.blockId}")
                .clearAndSetSemantics {
                    contentDescription = if (running) row.labelNow else row.label
                    role = Role.Button
                    if (checkActions.isNotEmpty()) customActions = checkActions
                    onClick {
                        onBlock(row.blockId)
                        true
                    }
                }.padding(horizontal = PlanMetrics.blockIcon.dp, vertical = PlanMetrics.sectionGap.dp),
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val swatch = measurables[0].measure(loose)
        val gap = PlanMetrics.sectionGap.dp.roundToPx()
        val trailingMin = measurables[2].minIntrinsicWidth(constraints.maxHeight)
        val textMin = measurables[1].minIntrinsicWidth(constraints.maxHeight)
        val trailingMax =
            (constraints.maxWidth - swatch.width - 2 * gap - textMin)
                .coerceAtLeast(trailingMin)
                .coerceAtMost((constraints.maxWidth - swatch.width - gap).coerceAtLeast(0))
        val trailing = measurables[2].measure(loose.copy(maxWidth = trailingMax))
        val secondGap = if (trailing.width > 0) gap else 0
        val text =
            measurables[1].measure(
                loose.copy(
                    maxWidth =
                        (
                            constraints.maxWidth - swatch.width - gap - secondGap -
                                trailing.width
                        ).coerceAtLeast(0),
                ),
            )
        val height = maxOf(swatch.height, text.height, trailing.height)
        layout(constraints.maxWidth, height) {
            swatch.placeRelative(0, (height - swatch.height) / 2)
            text.placeRelative(swatch.width + gap, (height - text.height) / 2)
            trailing.placeRelative(constraints.maxWidth - trailing.width, (height - trailing.height) / 2)
        }
    }
}

@Composable
private fun RowTrailing(row: ListRow) {
    Layout(content = {
        row.priorityTag?.let { TripPagePriorityTag(it, Modifier.testTag("trip.block.${row.blockId}.priority")) }
        row.status?.let {
            Icon(
                (
                    if (it ==
                        CheckStatus.Done
                    ) {
                        LucideGlyphs.Check
                    } else {
                        LucideGlyphs.SkipForward
                    }
                ).imageVector(PlanMetrics.listStatusStroke.toFloat()),
                null,
                Modifier.size(PlanMetrics.listStatusIcon.dp).testTag("trip.block.${row.blockId}.status"),
                tint = CfTheme.colors.dawnText,
            )
        }
    }, measurePolicy = TrailingMeasurePolicy)
}

private object TrailingMeasurePolicy : MeasurePolicy {
    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ): Int = measurables.maxOfOrNull { it.minIntrinsicWidth(height) } ?: 0

    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        val children = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val gap = PlanMetrics.rowGap.dp.roundToPx()
        val rowWidth = children.sumOf { it.width } + gap * (children.size - 1).coerceAtLeast(0)
        val horizontal = rowWidth <= constraints.maxWidth
        val width = if (horizontal) rowWidth else children.maxOf { it.width }
        val height =
            if (horizontal) {
                children.maxOfOrNull { it.height } ?: 0
            } else {
                children.sumOf { it.height } +
                    gap * (children.size - 1)
            }
        return layout(width, height) {
            var position = 0
            children.forEach {
                if (horizontal) {
                    it.placeRelative(position, (height - it.height) / 2)
                    position += it.width + gap
                } else {
                    it.placeRelative(width - it.width, position)
                    position += it.height + gap
                }
            }
        }
    }
}
