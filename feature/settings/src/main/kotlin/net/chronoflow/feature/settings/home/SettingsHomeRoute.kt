package net.chronoflow.feature.settings.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.feature.settings.SettingsDependencies
import net.chronoflow.feature.settings.settingsAttempt

@Composable
fun SettingsHomeRoute(
    viewModel: SettingsHomeViewModel,
    deps: SettingsDependencies,
    showUnbuilt: Boolean,
    onNavigate: (Route) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var formatter by remember(deps) { mutableStateOf<CopyLineFormatter?>(null) }
    LaunchedEffect(deps) { settingsAttempt({}) { formatter = deps.formatter() } }
    SettingsHomeScreen(SettingsHomeScreenState(ui, deps.clock, deps.zone, formatter, showUnbuilt), { event ->
        when (event) {
            is SettingsHomeEvent.Open -> onNavigate(event.route)
            SettingsHomeEvent.SignOut -> viewModel.signOut()
            SettingsHomeEvent.Stay -> viewModel.stay()
            SettingsHomeEvent.Discard -> viewModel.signOut(true)
        }
    }, modifier)
}
