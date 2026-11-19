package net.chronoflow.app.shell

import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import net.chronoflow.app.AppGraph
import net.chronoflow.app.LaunchPlatform
import net.chronoflow.app.SessionGraph
import net.chronoflow.app.platformTodaySource
import net.chronoflow.app.today.DbTodaySource
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.model.session.StaleSessionException
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.navigation.WizardMode
import net.chronoflow.core.navigation.shell.ShellTab
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.plan.PlanDecoder
import net.chronoflow.core.ui.clock.MinuteClock
import net.chronoflow.feature.today.TodayActions
import net.chronoflow.feature.today.TodayDependencies
import net.chronoflow.feature.today.TodayDependenciesKey
import net.chronoflow.feature.today.TodayRoute
import net.chronoflow.feature.today.TodayUiState
import net.chronoflow.feature.today.TodayViewModel
import net.chronoflow.feature.today.ui.TodayScreen
import java.time.Instant

@Composable
internal fun TodayTab(
    graph: AppGraph,
    session: SessionGraph?,
    holder: ShellViewModel,
) {
    Box(Modifier.fillMaxSize().testTag("route.today")) {
        val source =
            remember(graph, session) {
                if (session == null) {
                    platformTodaySource(graph)
                } else {
                    DbTodaySource(session, graph.dispatchers.io, PlanDecoder(graph.dispatchers.default)) {
                        graph.controller.mutationPermit(session.snapshot) ?: throw StaleSessionException()
                    }
                }
            }
        val sourceReady = rememberUpdatedState(source != null)
        ReportDrawnWhen(remember { { sourceReady.value } })
        if (source == null) {
            val clock = remember { MinuteClock(mutableStateOf(Instant.EPOCH)) }
            val language = if (LocalConfiguration.current.locales[0].language == "nl") Language.Nl else Language.En
            TodayScreen(TodayUiState.Loading, clock, language, null, TimeFormat.Value24, todayActions(holder) {}, {})
        } else {
            val dependencies =
                remember(graph, session, source) {
                    TodayDependencies(
                        source,
                        graph.planClock,
                        { CopyLineFormatter(graph.copyCatalogs.catalog()) },
                        graph.dispatchers.default,
                        graph.dispatchers.main,
                        LaunchPlatform.refreshProbe,
                    )
                }
            val owner = checkNotNull(LocalViewModelStoreOwner.current) as HasDefaultViewModelProviderFactory
            val extras =
                remember(owner, dependencies) {
                    MutableCreationExtras(
                        owner.defaultViewModelCreationExtras,
                    ).apply { set(TodayDependenciesKey, dependencies) }
                }
            val model: TodayViewModel =
                viewModel(
                    key = todayViewModelKey(session?.snapshot?.epoch),
                    factory = TodayViewModel.Factory,
                    extras = extras,
                )
            val actions =
                remember(model, holder, session) {
                    todayActions(holder) { model.refresh { session?.sync?.refreshNow() } }
                }
            TodayRoute(model, actions)
        }
    }
}

internal fun todayActions(
    holder: ShellViewModel,
    refresh: suspend () -> Unit,
) = TodayActions(
    openTrip = holder::openTrip,
    settings = { holder.select(ShellTab.Settings) },
    pastTrips = { holder.leaveTripPage(ShellTab.Trips) },
    create = { holder.push(Route.Wizard(WizardMode.Create)) },
    refresh = refresh,
)
