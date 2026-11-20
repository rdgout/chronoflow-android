package net.chronoflow.feature.wizard.sleep

import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.feature.wizard.shell.WizardMetrics

@Composable
internal fun WizardChronotypeGrid(content: @Composable () -> Unit) {
    Layout(
        content = content,
        modifier = Modifier.selectableGroup().testTag("wizard.sleep.chronotypes"),
    ) { measurables, constraints ->
        val columns = if (constraints.maxWidth < WizardMetrics.chronotypeSingleColumnBelow.dp.roundToPx()) 1 else 2
        val gap = PlanUITokens.space2.dp.roundToPx()
        val width = (constraints.maxWidth - gap * (columns - 1)) / columns
        val rows =
            measurables.chunked(columns).map { row ->
                val height = row.maxOf { it.maxIntrinsicHeight(width) }
                row.map { it.measure(Constraints.fixed(width, height)) }
            }
        layout(constraints.maxWidth, rows.sumOf { it.first().height } + gap * (rows.size - 1).coerceAtLeast(0)) {
            var y = 0
            rows.forEach { row ->
                row.forEachIndexed { column, cell -> cell.placeRelative(column * (width + gap), y) }
                y += row.first().height + gap
            }
        }
    }
}
