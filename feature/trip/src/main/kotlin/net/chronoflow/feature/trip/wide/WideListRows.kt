package net.chronoflow.feature.trip.wide

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.plan.PreparedDay
import net.chronoflow.core.plan.step.StepActions
import net.chronoflow.core.plan.trippage.DayListRows
import net.chronoflow.feature.trip.list.DayListRow
import net.chronoflow.feature.trip.list.ZoneChangeCard
import net.chronoflow.feature.trip.timeline.DayClock

@Composable
internal fun WideListRows(
    day: PreparedDay,
    rows: DayListRows,
    clock: DayClock?,
    selectedBlock: () -> String?,
    pin: WidePinSource,
    onBlock: (String) -> Unit,
    onCheck: (String, CheckStatus?) -> Unit,
    modifier: Modifier = Modifier,
) {
    PinnedDayList(day.id, pin = pin, modifier = modifier, revision = rows, rows = {
        rows.rows.forEach { row ->
            val selected = remember(row.blockId, selectedBlock) { { selectedBlock() == row.blockId } }
            if (row.zone) {
                ZoneChangeCard(row, onBlock, selected = selected)
            } else {
                DayListRow(
                    row,
                    clock,
                    onBlock,
                    actions = day.checkActions.byBlock[row.blockId] ?: StepActions.None,
                    onCheck = onCheck,
                    selected = selected,
                )
            }
        }
    })
}
