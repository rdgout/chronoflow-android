// Append slots so existing positional title arguments keep their meaning.
@file:Suppress("ktlint:compose:modifier-missing-check", "ktlint:compose:param-order-check")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.core.designsystem.system

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TimePickerDialogDefaults
import androidx.compose.material3.TimePickerDisplayMode
import androidx.compose.material3.TimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import net.chronoflow.core.designsystem.theme.CfTheme

@SuppressLint("ModifierParameter")
@Composable
fun CfDatePickerDialog(
    state: DatePickerState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmLabel: String,
    dismissLabel: String,
    title: String? = null,
    supporting: String? = null,
    confirmEnabled: Boolean = true,
    confirmTestTag: String = "cf.date.confirm",
    dismissTestTag: String = "cf.date.dismiss",
    modifier: Modifier = Modifier,
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
        modifier = modifier.semantics { testTagsAsResourceId = true },
        confirmButton = { DialogAction(CfDialogAction(confirmLabel, onConfirm), confirmTestTag, confirmEnabled) },
        dismissButton = { DialogAction(CfDialogAction(dismissLabel, onDismiss), dismissTestTag) },
        colors = colors,
    ) {
        if (title == null && supporting == null) {
            DatePicker(state, colors = colors)
        } else {
            DatePicker(state, title = {
                Column {
                    title?.let { Text(it) }
                    supporting?.let { Text(it, Modifier.testTag("cf.date.supporting"), color = c.mutedForeground) }
                }
            }, colors = colors)
        }
    }
}

@SuppressLint("ModifierParameter")
@Composable
fun CfTimePickerDialog(
    state: TimePickerState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmLabel: String,
    dismissLabel: String,
    title: String? = null,
    supporting: String? = null,
    confirmTestTag: String = "cf.time.confirm",
    dismissTestTag: String = "cf.time.dismiss",
    modifier: Modifier = Modifier,
    confirmEnabled: Boolean = true,
) {
    val c = CfTheme.colors
    val parentScheme = MaterialTheme.colorScheme
    val pickerScheme = remember(parentScheme, c.popover) { parentScheme.copy(surface = c.popover) }
    // M3 1.4.0 paints a default Surface over its containerColor background.
    MaterialTheme(colorScheme = pickerScheme) {
        TimePickerDialog(
            onDismissRequest = onDismiss,
            modifier = modifier.semantics { testTagsAsResourceId = true },
            confirmButton = { DialogAction(CfDialogAction(confirmLabel, onConfirm), confirmTestTag, confirmEnabled) },
            dismissButton = { DialogAction(CfDialogAction(dismissLabel, onDismiss), dismissTestTag) },
            title = {
                Column {
                    if (title == null) {
                        TimePickerDialogDefaults.Title(displayMode = TimePickerDisplayMode.Picker)
                    } else {
                        Text(title)
                    }
                    supporting?.let { Text(it, Modifier.testTag("cf.time.supporting"), color = c.mutedForeground) }
                }
            },
            containerColor = c.popover,
        ) {
            TimePicker(
                state,
                colors = cfTimePickerColors(c.popover),
            )
        }
    }
}

@Composable
internal fun cfTimePickerColors(
    container: androidx.compose.ui.graphics.Color,
): androidx.compose.material3.TimePickerColors {
    val c = CfTheme.colors
    return TimePickerDefaults.colors(
        containerColor = container,
        clockDialColor = c.muted,
        selectorColor = c.dawn,
        clockDialSelectedContentColor = c.dawnForeground,
        timeSelectorSelectedContainerColor = c.systemSelected,
        timeSelectorSelectedContentColor = c.dawnText,
        periodSelectorSelectedContainerColor = c.systemSelected,
        periodSelectorSelectedContentColor = c.dawnText,
    )
}
