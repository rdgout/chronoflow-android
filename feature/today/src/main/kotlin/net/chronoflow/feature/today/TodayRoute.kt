@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.feature.today

import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import net.chronoflow.core.designsystem.system.CfModalBottomSheet
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.model.config.ConfigurationRules
import net.chronoflow.core.model.config.LanguageChoice
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.ui.clock.MinuteClock
import net.chronoflow.core.ui.plan.StepSheetContent
import net.chronoflow.feature.today.ui.TodayScreen

@Composable
fun TodayRoute(
    viewModel: TodayViewModel,
    actions: TodayActions,
) {
    val resolved = ConfigurationRules.resolveLanguage(LanguageChoice.System, LocalConfiguration.current.locales[0])
    val language = if (resolved == AppLanguage.Nl) Language.Nl else Language.En
    LaunchedEffect(viewModel, language) { viewModel.setLanguage(language) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val drawingState = rememberUpdatedState(state)
    ReportDrawnWhen(remember(viewModel) { { drawingState.value !is TodayUiState.Loading } })
    val selection by viewModel.stepSheet.collectAsStateWithLifecycle()
    val sheet by viewModel.stepSheetState.collectAsStateWithLifecycle()
    val busy by viewModel.changingCheck.collectAsStateWithLifecycle()
    val formatter by viewModel.formatter.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val refresh by viewModel.refreshState.collectAsStateWithLifecycle()
    val now = viewModel.now.collectAsStateWithLifecycle()
    val clock = remember(now) { MinuteClock(now) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    TodayScreen(
        state,
        clock,
        language,
        formatter,
        preferences.timeFormat,
        actions,
        viewModel::openStep,
        refreshState = refresh,
        onRefresh = { scope.launch { actions.refresh() } },
        onDismissFeedback = viewModel::dismissFeedback,
        refreshProbe = viewModel.refreshProbe,
    )
    val selected = selection
    val content = sheet
    if (selected != null && content != null) {
        val dismiss: () -> Unit = {
            scope.launch { sheetState.hide() }.invokeOnCompletion { viewModel.dismissStep() }
        }
        CfModalBottomSheet(onDismissRequest = viewModel::dismissStep, sheetState = sheetState) {
            StepSheetContent(content, busy, { viewModel.setCheck(selected.blockId, it) }, dismiss)
        }
    }
}
