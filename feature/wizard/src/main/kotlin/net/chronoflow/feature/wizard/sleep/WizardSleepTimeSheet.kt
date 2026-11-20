@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.feature.wizard.sleep

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import net.chronoflow.core.designsystem.haptics.HapticEvent
import net.chronoflow.core.designsystem.haptics.LocalHaptics
import net.chronoflow.core.designsystem.system.CfTimePickerSheet
import net.chronoflow.core.ui.R
import net.chronoflow.core.wizard.model.WizardClockRounding
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.times.wizardPickerSemantics
import net.chronoflow.feature.wizard.ui.WizardCopy
import net.chronoflow.feature.wizard.ui.resolve

@Composable
internal fun WizardSleepTimeSheet(
    ui: WizardTimeSheetUi,
    onEvent: (WizardEvent) -> Unit,
) {
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    val haptics = LocalHaptics.current
    key(ui.field) {
        CfTimePickerSheet(
            title = ui.label.resolve(),
            subtitle =
                ui.subtitle?.resolve()
                    ?: if (ui.usual) stringResource(R.string.settings_on_a_normal_work_or_school_day) else null,
            footnote = if (ui.minutePrecision) null else stringResource(R.string.android_wizard_time_sheet_steps),
            initialHour = ui.initialHour,
            initialMinute = ui.initialMinute,
            is24Hour = is24Hour,
            confirmLabel = WizardCopy("Done").resolve(),
            dismissLabel = WizardCopy("Cancel").resolve(),
            onConfirm = { hour, minute, touched ->
                if (if (ui.minutePrecision) {
                        touched && clockString(hour, minute) != ui.stored
                    } else {
                        WizardClockRounding.change(ui.stored, hour, minute, touched) !=
                            null
                    }
                ) {
                    haptics.perform(HapticEvent.Selection)
                }
                onEvent(WizardEvent.ConfirmTimeSheet(ui.field, hour, minute, touched))
            },
            onDismiss = { onEvent(WizardEvent.DismissTimeSheet) },
            modifier = Modifier.wizardPickerSemantics(null, is24Hour),
            testTag = "wizard.time-sheet",
        )
    }
}
