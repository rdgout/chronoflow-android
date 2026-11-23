package net.chronoflow.feature.settings.security

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.system.CfSnackbarHost
import net.chronoflow.core.ui.l10n.generated.CopyIndex
import net.chronoflow.core.ui.l10n.generated.CopyTable
import net.chronoflow.feature.settings.SettingsDebugCounters

@Composable
internal fun SecurityFeedbackHost(
    state: StateFlow<SecurityUiState>,
    onPresent: (Long) -> Unit,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val activity = LocalActivity.current
    val leaving by rememberUpdatedState(onLeave)
    val presented by rememberUpdatedState(onPresent)
    DisposableEffect(state, activity) {
        onDispose { if (activity?.isChangingConfigurations != true) leaving() }
    }
    val snackbar = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(state, snackbar, resources) {
        state.map { it.feedback }.filterNotNull().distinctUntilChanged().collectLatest { feedback ->
            snackbar.showSnackbar(
                SecuritySnackbar(
                    feedback.id,
                    resources.getString(checkNotNull(CopyIndex.find(CopyTable.Settings, feedback.key)).resId),
                ),
            )
        }
    }
    Box(modifier.fillMaxSize()) {
        content()
        CfSnackbarHost(
            snackbar,
            Modifier
                .align(
                    Alignment.BottomCenter,
                ).padding(PlanUITokens.space4.dp)
                .testTag("settings.security.feedback"),
            onPresented = { data ->
                val id = (data.visuals as? SecuritySnackbar)?.id
                if (id != null) {
                    SettingsDebugCounters.securityPresented()
                    presented(id)
                }
            },
        )
    }
}

private data class SecuritySnackbar(
    val id: Long,
    override val message: String,
) : SnackbarVisuals {
    override val actionLabel: String? = null
    override val withDismissAction = false
    override val duration = SnackbarDuration.Long
}
