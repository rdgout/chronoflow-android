@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.feature.wizard.answers

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.flow.drop
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.designsystem.haptics.HapticEvent
import net.chronoflow.core.designsystem.haptics.LocalHaptics
import net.chronoflow.core.designsystem.system.CfTimePicker
import net.chronoflow.core.ui.window.LocalWindowSize
import net.chronoflow.core.wizard.model.WizardDateTime
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardMetrics
import net.chronoflow.feature.wizard.sleep.clockString
import net.chronoflow.feature.wizard.times.wizardPickerSemantics

@Composable
internal fun WizardWakeControl(
    ui: WizardAnswerSheetUi,
    onEvent: (WizardEvent) -> Unit,
) {
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    val compact = LocalWindowSize.current.height.value < WizardMetrics.shortPickerWindow
    val haptics = LocalHaptics.current
    val events = rememberUpdatedState(onEvent)
    key(ui.state.prompt.id, is24Hour) {
        val raw = ui.state.values["latest_wake"].clockString()
        val state =
            rememberTimePickerState(
                if (WizardDateTime.isTime(raw)) raw!!.take(2).toInt() else 12,
                if (WizardDateTime.isTime(raw)) raw!!.takeLast(2).toInt() else 0,
                is24Hour,
            )
        LaunchedEffect(state) {
            snapshotFlow { state.hour to state.minute }.drop(1).collect { (hour, minute) ->
                events.value(WizardEvent.UpdateAnswer("latest_wake", JsonPrimitive(clockString(hour, minute))))
                haptics.perform(HapticEvent.Selection)
            }
        }
        WizardAnswerCard(Modifier.testTag("wizard.answer.latest_wake.card")) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CfTimePicker(
                    state,
                    Modifier.testTag("wizard.answer.latest_wake").wizardPickerSemantics(null, state.is24hour),
                    compact,
                )
            }
        }
    }
}
