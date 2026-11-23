package net.chronoflow.feature.wizard.answers

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfDivider
import net.chronoflow.feature.wizard.components.WizardTimeRow
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardMetrics
import net.chronoflow.feature.wizard.sleep.clockString
import net.chronoflow.feature.wizard.ui.WizardCopy

@Composable
internal fun WizardFreeDayControl(
    ui: WizardAnswerSheetUi,
    onEvent: (WizardEvent) -> Unit,
) {
    if (!ui.declared) return
    WizardAnswerCard {
        WizardTimeRow(
            WizardCopy("Bedtime"),
            ui.state.values["free_day_bed"].clockString(),
            ui.timeFormat,
            { onEvent(WizardEvent.OpenTimeSheet("free_day_bed")) },
            "wizard.answer.time.free_day_bed",
            minHeight = WizardMetrics.sheetRowMinHeight.dp,
        )
        CfDivider()
        WizardTimeRow(
            WizardCopy("Wake-up"),
            ui.state.values["free_day_wake"].clockString(),
            ui.timeFormat,
            { onEvent(WizardEvent.OpenTimeSheet("free_day_wake")) },
            "wizard.answer.time.free_day_wake",
            minHeight = WizardMetrics.sheetRowMinHeight.dp,
        )
    }
}
