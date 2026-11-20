@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.feature.wizard.times

import android.text.format.DateFormat
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import net.chronoflow.core.designsystem.haptics.HapticEvent
import net.chronoflow.core.designsystem.haptics.LocalHaptics
import net.chronoflow.core.designsystem.system.CfDatePickerDialog
import net.chronoflow.core.designsystem.system.CfTimePickerDialog
import net.chronoflow.core.wizard.model.WizardPickerDates
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.ui.WizardCopy
import net.chronoflow.feature.wizard.ui.resolve
import java.time.LocalDate

@Immutable
data class WizardPicker(
    val index: Int,
    val arrival: Boolean,
    val kind: WizardPickerKind,
)

internal class WizardSelectableDates(
    private val minimum: LocalDate?,
) : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
        WizardPickerDates.isSelectable(utcTimeMillis, minimum?.atStartOfDay())

    override fun isSelectableYear(year: Int): Boolean =
        WizardPickerDates.isSelectableYear(year, minimum?.atStartOfDay())
}

@Composable
internal fun WizardFlightPickers(
    ui: WizardFlightPickerUi,
    field: WizardDateTimeFieldUi,
    onEvent: (WizardEvent) -> Unit,
    onDismiss: () -> Unit,
) {
    val picker = ui.picker
    val haptics = LocalHaptics.current
    val done = WizardCopy("Done").resolve()
    val cancel = WizardCopy("Cancel").resolve()
    val title =
        WizardCopy(
            when {
                picker.kind == WizardPickerKind.Date && field.arrival -> "Arrival date"
                picker.kind == WizardPickerKind.Date -> "Departure date"
                field.arrival -> "Arrival time"
                else -> "Departure time"
            },
        ).resolve()
    val supporting = field.zone.resolve()
    if (picker.kind == WizardPickerKind.Date) {
        val selectable = remember(field.minimumDate) { WizardSelectableDates(field.minimumDate) }
        val state =
            rememberDatePickerState(
                initialSelectedDateMillis = field.datePart?.let { WizardPickerDates.utcMillis(LocalDate.parse(it)) },
                initialDisplayedMonthMillis = WizardPickerDates.utcMillis(ui.initialDate),
                selectableDates = selectable,
            )
        CfDatePickerDialog(
            state,
            onDismiss,
            {
                state.selectedDateMillis?.let {
                    onEvent(WizardEvent.SetDatePart(field.index, field.arrival, WizardPickerDates.localDate(it)))
                    haptics.perform(HapticEvent.Selection)
                    onDismiss()
                }
            },
            done,
            cancel,
            title,
            supporting = supporting,
            confirmEnabled = state.selectedDateMillis != null,
            confirmTestTag = "wizard.picker.done",
            dismissTestTag = "wizard.picker.cancel",
            modifier = Modifier.testTag("wizard.date-dialog").wizardPickerSemantics(field.minimumDate, null),
        )
    } else {
        val state =
            rememberTimePickerState(
                ui.initialHour,
                ui.initialMinute,
                DateFormat.is24HourFormat(LocalContext.current),
            )
        CfTimePickerDialog(
            state,
            onDismiss,
            {
                onEvent(WizardEvent.SetTimePart(field.index, field.arrival, state.hour, state.minute))
                haptics.perform(HapticEvent.Selection)
                onDismiss()
            },
            done,
            cancel,
            title,
            supporting = supporting,
            confirmTestTag = "wizard.picker.done",
            dismissTestTag = "wizard.picker.cancel",
            modifier = Modifier.testTag("wizard.time-dialog").wizardPickerSemantics(null, state.is24hour),
        )
    }
}
