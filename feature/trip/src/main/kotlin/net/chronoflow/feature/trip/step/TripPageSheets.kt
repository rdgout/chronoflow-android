@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.feature.trip.step

import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import net.chronoflow.core.designsystem.system.CfModalBottomSheet
import net.chronoflow.core.navigation.TripSheet
import net.chronoflow.core.ui.plan.LegendSheetContent
import net.chronoflow.core.ui.plan.PriorityLegendContent
import net.chronoflow.core.ui.plan.StepSheetContent
import net.chronoflow.feature.trip.page.TripPageViewModel

@Suppress("ktlint:compose:vm-forwarding-check", "ktlint:compose:modifier-missing-check")
@Composable
fun TripPageSheets(
    vm: TripPageViewModel,
    onWizard: (net.chronoflow.core.navigation.WizardMode) -> Unit = {},
) {
    val sheet by vm.sheet.collectAsStateWithLifecycle()
    val state by vm.stepSheet.collectAsStateWithLifecycle()
    val busy by vm.checkBusy.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val dismiss: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { vm.dismissSheet() }
    }
    when (val selection = sheet) {
        is TripSheet.Step -> {
            if (state != null) {
                CfModalBottomSheet(vm::dismissSheet, Modifier.testTag("trip.sheet"), sheetState = sheetState) {
                    StepSheetContent(checkNotNull(state), busy, { vm.setCheck(selection.blockId, it) }, dismiss)
                }
            }
        }

        TripSheet.Legend -> {
            CfModalBottomSheet(vm::dismissSheet, Modifier.testTag("trip.sheet"), sheetState = sheetState) {
                PriorityLegendContent(dismiss)
            }
        }

        TripSheet.StepsLegend -> {
            CfModalBottomSheet(vm::dismissSheet, Modifier.testTag("trip.sheet"), sheetState = sheetState) {
                LegendSheetContent(dismiss)
            }
        }

        TripSheet.TripActions -> {
            val actions by vm.tripActions.collectAsStateWithLifecycle()
            actions?.let { ui ->
                val failure by vm.actionFailure.collectAsStateWithLifecycle()
                net.chronoflow.feature.trip.actions.TripActionsWindow(
                    ui,
                    failure,
                    vm::startAction,
                    vm::dismissSheet,
                    vm.wizardRequests,
                    onWizard,
                )
            }
        }

        null -> {}
    }
}
