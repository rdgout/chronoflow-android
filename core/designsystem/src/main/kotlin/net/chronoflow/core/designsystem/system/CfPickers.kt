@file:Suppress("ktlint:compose:modifier-missing-check")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.core.designsystem.system

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TimePickerDialogDefaults
import androidx.compose.material3.TimePickerDisplayMode
import androidx.compose.material3.TimePickerState
import androidx.compose.runtime.Composable
import net.chronoflow.core.designsystem.theme.CfTheme

@Composable
fun CfDatePickerDialog(
    state: DatePickerState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmLabel: String,
    dismissLabel: String,
    title: String? = null,
) {
    val c = CfTheme.colors
    val colors =
        DatePickerDefaults.colors(
            containerColor = c.popover,
            selectedDayContainerColor = c.dawn,
            selectedDayContentColor = c.dawnForeground,
            todayDateBorderColor = c.dawn,
            todayContentColor = c.dawnText,
            selectedYearContainerColor = c.dawn,
            selectedYearContentColor = c.dawnForeground,
            headlineContentColor = c.foreground,
        )
    DatePickerDialog(
        onDismiss,
        confirmButton = { DialogAction(CfDialogAction(confirmLabel, onConfirm), "cf.date.confirm") },
        dismissButton = { DialogAction(CfDialogAction(dismissLabel, onDismiss), "cf.date.dismiss") },
        colors = colors,
    ) {
        if (title == null) {
            DatePicker(state, colors = colors)
        } else {
            DatePicker(state, title = { Text(title) }, colors = colors)
        }
    }
}

@Composable
fun CfTimePickerDialog(
    state: TimePickerState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmLabel: String,
    dismissLabel: String,
    title: String? = null,
) {
    val c = CfTheme.colors
    TimePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = { DialogAction(CfDialogAction(confirmLabel, onConfirm), "cf.time.confirm") },
        dismissButton = { DialogAction(CfDialogAction(dismissLabel, onDismiss), "cf.time.dismiss") },
        title = {
            if (title == null) {
                TimePickerDialogDefaults.Title(displayMode = TimePickerDisplayMode.Picker)
            } else {
                Text(title)
            }
        },
        containerColor = c.popover,
    ) {
        TimePicker(
            state,
            colors =
                TimePickerDefaults.colors(
                    clockDialColor = c.muted,
                    selectorColor = c.dawn,
                    clockDialSelectedContentColor = c.dawnForeground,
                    timeSelectorSelectedContainerColor = c.systemSelected,
                    timeSelectorSelectedContentColor = c.dawnText,
                    periodSelectorSelectedContainerColor = c.systemSelected,
                    periodSelectorSelectedContentColor = c.dawnText,
                ),
        )
    }
}
