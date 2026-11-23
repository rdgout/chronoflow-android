@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.feature.wizard.answers

import android.text.format.DateFormat
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import net.chronoflow.core.designsystem.haptics.HapticEvent
import net.chronoflow.core.designsystem.haptics.LocalHaptics
import net.chronoflow.core.designsystem.system.CfDatePickerDialog
import net.chronoflow.core.designsystem.system.CfTimePickerDialog
import net.chronoflow.core.wizard.model.WizardDateTime
import net.chronoflow.core.wizard.model.WizardPickerDates
import net.chronoflow.feature.wizard.times.WizardSelectableDates
import net.chronoflow.feature.wizard.times.wizardPickerSemantics
import net.chronoflow.feature.wizard.ui.WizardCopy
import net.chronoflow.feature.wizard.ui.resolve

@Composable
internal fun WizardBusyPickers(
    stage: Int,
    from: String,
    to: String,
    zone: WizardCopy,
    onDismiss: () -> Unit,
    onFrom: (String) -> Unit,
    onTo: (String) -> Unit,
    onDone: (String) -> Unit,
) {
    val start = WizardDateTime.date(from) ?: return
    val value = if (stage < 2) start else WizardDateTime.date(to) ?: start.plusHours(1)
    val title = WizardCopy(if (stage < 2) "From" else "To").resolve()
    val supporting = zone.resolve()
    val done = WizardCopy("Done").resolve()
    val cancel = WizardCopy("Cancel").resolve()
    val haptics = LocalHaptics.current
    val phone24 = DateFormat.is24HourFormat(LocalContext.current)
    key(stage) {
        if (stage == 0 || stage == 2) {
            val minimum = if (stage == 2) start.toLocalDate() else null
            val selectable = remember(minimum) { WizardSelectableDates(minimum) }
            val state =
                rememberDatePickerState(
                    initialSelectedDateMillis = WizardPickerDates.utcMillis(value.toLocalDate()),
                    selectableDates = selectable,
                )
            CfDatePickerDialog(
                state,
                onDismiss,
                {
                    state.selectedDateMillis?.let { millis ->
                        val next = WizardDateTime.string(value.with(WizardPickerDates.localDate(millis)))
                        if (stage == 0) onFrom(next) else onTo(next)
                    }
                },
                done,
                cancel,
                title,
                supporting = supporting,
                confirmEnabled = state.selectedDateMillis != null,
                confirmTestTag = "wizard.picker.done",
                dismissTestTag = "wizard.picker.cancel",
                modifier = Modifier.testTag("wizard.date-dialog").wizardPickerSemantics(minimum, null),
            )
        } else {
            val state = rememberTimePickerState(value.hour, value.minute, phone24)
            WizardBusyTimeDialog(
                state,
                value,
                start,
                stage == 3,
                title,
                supporting,
                onDismiss = onDismiss,
                onConfirm = { next ->
                    haptics.perform(HapticEvent.Selection)
                    if (stage == 1) onFrom(next) else onDone(next)
                },
            )
        }
    }
}

@Composable
internal fun WizardBusyTimeDialog(
    state: androidx.compose.material3.TimePickerState,
    date: java.time.LocalDateTime,
    start: java.time.LocalDateTime,
    ending: Boolean,
    title: String,
    supporting: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val next = date.withHour(state.hour).withMinute(state.minute)
    CfTimePickerDialog(
        state,
        onDismiss,
        { onConfirm(WizardDateTime.string(next)) },
        WizardCopy("Done").resolve(),
        WizardCopy("Cancel").resolve(),
        title,
        supporting = supporting,
        confirmTestTag = "wizard.picker.done",
        dismissTestTag = "wizard.picker.cancel",
        modifier = Modifier.testTag("wizard.time-dialog").wizardPickerSemantics(null, state.is24hour),
        confirmEnabled = !ending || next > start,
    )
}
