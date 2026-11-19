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
fun TripPageSheets(vm: TripPageViewModel) {
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

        TripSheet.TripActions, null -> {}
    }
}
