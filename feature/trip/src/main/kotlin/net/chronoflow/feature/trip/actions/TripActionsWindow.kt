@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.feature.trip.actions

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.chronoflow.core.designsystem.system.CfModalBottomSheet
import net.chronoflow.core.designsystem.system.CfSnackbarHost
import net.chronoflow.core.navigation.WizardMode
import net.chronoflow.core.plan.actions.TripAction

@Composable
internal fun TripActionsWindow(
    ui: TripActionsUi,
    failure: TripActionFailure?,
    onAction: (TripAction) -> Unit,
    onDismiss: () -> Unit,
    wizardRequests: Flow<WizardMode> = emptyFlow(),
    onWizard: (WizardMode) -> Unit = {},
) {
    val busy by rememberUpdatedState(ui.availability.busy)
    val dismiss by rememberUpdatedState(onDismiss)
    val openWizard by rememberUpdatedState(onWizard)
    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { it != SheetValue.Hidden || !busy },
        )
    val scope = rememberCoroutineScope()
    val host = remember { SnackbarHostState() }
    val message = failure?.let { stringResource(it.message) }
    LaunchedEffect(failure) {
        if (message != null) host.showSnackbar(message, duration = SnackbarDuration.Short)
    }
    LaunchedEffect(wizardRequests) {
        wizardRequests.collect { mode ->
            // The request can arrive before the busy state has reached this composition.
            snapshotFlow { busy }.first { !it }
            sheetState.hide()
            dismiss()
            openWizard(mode)
        }
    }
    CfModalBottomSheet({ if (!busy) dismiss() }, Modifier.testTag("trip.sheet"), sheetState = sheetState) {
        Column {
            TripActionsContent(ui, onAction, {
                if (!busy) {
                    scope.launch {
                        sheetState.hide()
                        dismiss()
                    }
                }
            })
            CfSnackbarHost(
                host,
                Modifier.testTag("trip.actions.failure"),
                onPresented = { TripActionsDebugCounters.presented() },
            )
        }
    }
}
