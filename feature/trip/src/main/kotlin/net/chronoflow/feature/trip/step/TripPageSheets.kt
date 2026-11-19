@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.feature.trip.step

import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
    val actions by vm.tripActions.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val dismiss: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { vm.dismissSheet() }
    }
    when (val selection = sheet) {
        is TripSheet.Step -> {
            if (state != null) {
                CfModalBottomSheet(vm::dismissSheet, sheetState = sheetState) {
                    StepSheetContent(checkNotNull(state), busy, { vm.setCheck(selection.blockId, it) }, dismiss)
                }
            }
        }

        TripSheet.Legend -> {
            CfModalBottomSheet(vm::dismissSheet, sheetState = sheetState) { PriorityLegendContent(dismiss) }
        }

        TripSheet.StepsLegend -> {
            CfModalBottomSheet(vm::dismissSheet, sheetState = sheetState) { LegendSheetContent(dismiss) }
        }

        TripSheet.TripActions -> {
            actions?.let { ui ->
                val actionState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                val close: () -> Unit = { scope.launch { actionState.hide() }.invokeOnCompletion { vm.dismissSheet() } }
                CfModalBottomSheet(vm::dismissSheet, sheetState = actionState) {
                    net.chronoflow.feature.trip.actions.TripActionsContent(ui, { action ->
                        if (ui.availability.enabled(action)) {
                            scope.launch {
                                try {
                                    actionState.hide()
                                } finally {
                                    vm.dismissSheet()
                                }
                                onWizard(
                                    if (action == net.chronoflow.core.plan.actions.TripAction.Edit) {
                                        net.chronoflow.core.navigation.WizardMode
                                            .Edit(ui.tripId)
                                    } else {
                                        net.chronoflow.core.navigation.WizardMode
                                            .Duplicate(ui.tripId)
                                    },
                                )
                            }
                        }
                    }, close)
                }
            }
        }

        null -> {}
    }
}
