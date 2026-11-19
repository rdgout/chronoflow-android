package net.chronoflow.app.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import net.chronoflow.app.ChronoFlowApp
import net.chronoflow.app.LaunchPlatform
import net.chronoflow.app.trip.TripPageRoute
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.navigation.WizardMode
import net.chronoflow.core.navigation.shell.ShellTab
import net.chronoflow.core.sync.refresh.RefreshFeedbackState
import net.chronoflow.feature.trips.TripsContent
import net.chronoflow.feature.trips.TripsDependenciesKey
import net.chronoflow.feature.trips.TripsScreen
import net.chronoflow.feature.trips.TripsUiState
import net.chronoflow.feature.trips.TripsViewModel

@Composable
internal fun AppShell(
    root: Route,
    onRootBack: () -> Unit,
    placeholder: @Composable (Route) -> Unit,
) {
    val graph = (LocalContext.current.applicationContext as ChronoFlowApp).graph
    val sessionGraph by graph.sessionGraphs.current.collectAsStateWithLifecycle()
    val render = rememberUpdatedState(placeholder)
    val holder = rememberShellViewModel(root)
    LaunchedEffect(holder) { LaunchPlatform.shellStart()?.let(holder::seed) }
    AppShellScaffold(
        holder,
        onRootBack = onRootBack,
        today = { render.value(Route.Today) },
        trips = {
            val session = sessionGraph
            if (session == null) {
                val language = if (LocalConfiguration.current.locales[0].language == "nl") Language.Nl else Language.En
                TripsContent(
                    TripsUiState.Loading,
                    RefreshFeedbackState(),
                    language,
                    {},
                    {},
                    holder::openTrip,
                    { holder.push(Route.Wizard(WizardMode.Create)) },
                    refreshProbe = LaunchPlatform.refreshProbe,
                )
            } else {
                val owner = checkNotNull(LocalViewModelStoreOwner.current) as HasDefaultViewModelProviderFactory
                val deps = remember(graph, session) { SessionTripsDependencies(graph, session) }
                val extras =
                    remember(owner, deps) {
                        MutableCreationExtras(
                            owner.defaultViewModelCreationExtras,
                        ).apply { set(TripsDependenciesKey, deps) }
                    }
                TripsScreen(
                    viewModel(factory = TripsViewModel.Factory, extras = extras),
                    holder::openTrip,
                    { holder.push(Route.Wizard(WizardMode.Create)) },
                )
            }
        },
        settings = { render.value(Route.Settings) },
        page = { route ->
            if (route is Route.TripPage) {
                TripPageRoute(route, { if (!holder.back()) onRootBack() }, { holder.select(ShellTab.Settings) })
            } else {
                render.value(route)
            }
        },
        overlay = { render.value(it) },
    )
}
