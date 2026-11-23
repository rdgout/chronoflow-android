package net.chronoflow.feature.wizard.answers

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.feature.wizard.components.WizardTimeRow
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardMetrics
import net.chronoflow.feature.wizard.sleep.clockString
import net.chronoflow.feature.wizard.ui.WizardCopy

@Composable
internal fun WizardRoomControl(
    ui: WizardAnswerSheetUi,
    onEvent: (WizardEvent) -> Unit,
) {
    if (ui.state.values["stay.bed_available_mode"] != JsonPrimitive("declared")) return
    WizardAnswerCard {
        WizardTimeRow(
            WizardCopy("wizard.room.from"),
            ui.state.values["stay.bed_available_from_local"]
                .clockString()
                ?.takeLast(5),
            ui.timeFormat,
            { onEvent(WizardEvent.OpenTimeSheet("stay.bed_available_from_local")) },
            "wizard.answer.room-time",
            minHeight = WizardMetrics.sheetRowMinHeight.dp,
            enabled = ui.roomArrival != null,
        )
    }
}
