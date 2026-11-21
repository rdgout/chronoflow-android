package net.chronoflow.feature.settings.plan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.system.CfPullToRefreshBox
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.ui.R
import net.chronoflow.feature.settings.components.SettingsPage

data class PlanScreenState(
    val plan: SettingsPlanState = SettingsPlanState(null, null, false, false),
    val line: SettingsPlanLine? = null,
    val refreshing: Boolean = false,
)

enum class PlanEvent { Back, Refresh }

@Composable
fun PlanScreen(
    state: PlanScreenState,
    onEvent: (PlanEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    CfPullToRefreshBox(state.refreshing, { onEvent(PlanEvent.Refresh) }, modifier) {
        SettingsPage(
            R.string.settings_plan_credits,
            { onEvent(PlanEvent.Back) },
            "settings.plan.screen",
            spacing = PlanUITokens.space5.dp,
        ) {
            PlanContent(state, { onEvent(PlanEvent.Refresh) })
        }
    }
}

@Composable
fun PlanRoute(
    viewModel: PlanViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    val language =
        Language.of(
            LocalResources.current.configuration.locales[0]
                .language,
        )
    LaunchedEffect(viewModel, language) { viewModel.setLanguage(language) }
    PlanScreen(state, { if (it == PlanEvent.Back) onBack() else viewModel.refresh() }, modifier)
}
