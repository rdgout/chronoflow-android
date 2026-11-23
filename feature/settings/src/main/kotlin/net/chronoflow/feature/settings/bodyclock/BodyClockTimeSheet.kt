@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.feature.settings.bodyclock

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import net.chronoflow.core.designsystem.system.CfTimePickerSheet
import net.chronoflow.core.ui.R

@Composable
internal fun BodyClockTimeSheet(
    state: BodyClockTimeSheet,
    onEvent: (BodyClockEvent) -> Unit,
) {
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    val title =
        when (state.field) {
            "habitualBed" -> R.string.settings_usual_bedtime
            "habitualWake" -> R.string.settings_usual_wake_time
            "freeDayBed" -> R.string.settings_free_day_bedtime
            else -> R.string.settings_free_day_wake_time
        }
    key(state.field) {
        CfTimePickerSheet(
            title = stringResource(title),
            subtitle =
                stringResource(
                    if (state.field.startsWith("freeDay")) {
                        R.string.settings_on_free_days
                    } else {
                        R.string.settings_on_a_normal_work_or_school_day
                    },
                ),
            footnote = stringResource(R.string.android_wizard_time_sheet_steps),
            initialHour = state.initialHour,
            initialMinute = state.initialMinute,
            is24Hour = is24Hour,
            confirmLabel = stringResource(R.string.settings_done),
            dismissLabel = stringResource(R.string.settings_cancel),
            onConfirm = { hour, minute, _ -> onEvent(BodyClockEvent.ConfirmTime(state.field, hour, minute)) },
            onDismiss = { onEvent(BodyClockEvent.DismissTime) },
            testTag = "settings.bodyClock.time-sheet",
        )
    }
}
