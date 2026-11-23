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
import net.chronoflow.app.guest.GuestContinuationRunner
import net.chronoflow.app.trip.TripPageRoute
import net.chronoflow.core.data.guest.GuestContinuationRepository
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.navigation.Route
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
    LaunchedEffect(graph, holder) {
        net.chronoflow.app.reminders
            .ReminderTapRouting(
                graph.reminderTaps,
                graph.controller::activeSession,
                holder::openReminder,
                graph.dispatchers.main,
            ).run()
    }
    val wizardDependencies = sessionGraph?.wizard
    val launcher = holder.wizardLauncher(wizardDependencies, graph.dispatchers.io)
    val wizardLocale = rememberUpdatedState(LocalConfiguration.current.locales[0])
    LaunchedEffect(sessionGraph?.epoch) {
        val session = sessionGraph ?: return@LaunchedEffect
        val epoch = session.epoch
        if (LaunchPlatform.prepareWizard(graph, epoch, wizardLocale.value)) launcher.open()
        if (graph.wizardRecoveryCheck.shouldOpen(epoch)) launcher.open()
        GuestContinuationRunner(
            GuestContinuationRepository(session.database, session.permit),
            gate = graph.readiness::gate,
            isCurrent = { graph.controller.activeSession() == session.snapshot },
            onTrip = holder::wizardSaved,
            onTrips = holder::guestTrips,
            onMemberWizard = launcher::open,
        ).run()
    }
    LaunchedEffect(holder) { LaunchPlatform.shellStart()?.let(holder::seed) }
    androidx.compose.runtime.CompositionLocalProvider(
        net.chronoflow.core.ui.plan.LocalRefusedChanges provides sessionGraph?.refusedChanges,
    ) {
        AppShellScaffold(
            holder,
            onRootBack = onRootBack,
            overlayOwnsBack = holder.state.overlay.let { it is Route.Wizard },
            today = { TodayTab(graph, sessionGraph, holder) },
            trips = {
                val session = sessionGraph
                if (session == null) {
                    val dutch = LocalConfiguration.current.locales[0].language == "nl"
                    val language = if (dutch) Language.Nl else Language.En
                    TripsContent(
                        TripsUiState.Loading,
                        RefreshFeedbackState(),
                        language,
                        {},
                        {},
                        holder::openTrip,
                        launcher::open,
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
                        launcher::open,
                    )
                }
            },
            settings = {
                net.chronoflow.app.settings.SettingsEntry(
                    graph,
                    sessionGraph,
                    Route.Settings,
                    { if (!holder.back()) onRootBack() },
                    holder::push,
                )
            },
            page = { route ->
                if (route is Route.TripPage) {
                    TripPageRoute(
                        route,
                        { if (!holder.back()) onRootBack() },
                        { holder.select(ShellTab.Settings) },
                        onTrips = { holder.leaveTripPage(ShellTab.Trips) },
                        onToday = { holder.leaveTripPage(ShellTab.Today) },
                        onRouteChange = { holder.replaceRoute(route, it) },
                        onWizard = { holder.push(Route.Wizard(it)) },
                        preparer = launcher,
                    )
                } else if (route is Route.Walkthrough) {
                    net.chronoflow.app.onboarding
                        .WalkthroughEntry(graph, replay = true) { holder.back() }
                } else if (route == Route.Account || route == Route.Profile ||
                    route == Route.Security || route == Route.Password || route == Route.TwoFactorSettings ||
                    route == Route.Language || route == Route.Preferences || route == Route.BodyClock ||
                    route == Route.Passkeys ||
                    route == Route.Plan || route == Route.Reminders
                ) {
                    net.chronoflow.app.settings.SettingsEntry(
                        graph,
                        sessionGraph,
                        route,
                        { if (!holder.back()) onRootBack() },
                        holder::push,
                    )
                } else {
                    render.value(route)
                }
            },
            overlay = { route ->
                if (route is Route.Wizard) {
                    val startOverride = graph.wizardStartOverride
                    val sessionDeps = sessionGraph?.wizard
                    val deps =
                        remember(sessionDeps, startOverride) {
                            sessionDeps?.let { startOverride?.dependencies(it) ?: it }
                        }
                    WizardOverlay(route.mode, deps, startOverride?.presentation, holder::closeWizard)
                } else {
                    render.value(route)
                }
            },
        )
    }
}
