@file:Suppress("ktlint:standard:max-line-length", "ktlint:compose:modifier-missing-check")

package net.chronoflow.feature.wizard.shell

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import net.chronoflow.core.designsystem.system.CfAlertDialog
import net.chronoflow.core.designsystem.system.CfDialogAction
import net.chronoflow.feature.wizard.ui.WizardCopy
import net.chronoflow.feature.wizard.ui.WizardUiState
import net.chronoflow.feature.wizard.ui.resolve

@Composable
internal fun WizardBackHandling(
    state: WizardUiState,
    onEvent: (WizardEvent) -> Unit,
) {
    BackHandler(
        enabled =
            (state.firstScreen || state.step == WizardStepKey.Where) && state.sheet == null &&
                state.sleep?.openTime == null && state.fineTune?.openTime == null && state.answerSheet == null &&
                !state.discardDialogVisible,
    ) {
        onEvent(WizardEvent.RequestClose)
    }
    BackHandler(
        enabled =
            state.step == WizardStepKey.Checking && state.checking?.failure == null && !state.discardDialogVisible,
    ) {
        onEvent(WizardEvent.Back)
    }
    BackHandler(enabled = state.sheet != null && !state.discardDialogVisible) { onEvent(WizardEvent.CancelAirport) }
    BackHandler(enabled = state.discardDialogVisible) { onEvent(WizardEvent.DismissDiscard) }
    if (state.discardDialogVisible) {
        CfAlertDialog(
            title = state.discardTitle.resolve(),
            text = state.discardMessage.resolve(),
            confirm =
                CfDialogAction(
                    state.discardConfirmLabel.resolve(),
                    { onEvent(WizardEvent.ConfirmDiscard) },
                    destructive = true,
                ),
            dismiss = CfDialogAction(WizardCopy("Keep editing").resolve(), { onEvent(WizardEvent.DismissDiscard) }),
            onDismissRequest = { onEvent(WizardEvent.DismissDiscard) },
        )
    }
}
