package net.chronoflow.app.trip

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import net.chronoflow.app.AppGraphKey
import net.chronoflow.app.ChronoFlowApp
import net.chronoflow.app.LaunchPlatform
import net.chronoflow.core.data.sync.ConnectivitySyncPathMonitor
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.plan.FixedPlanClock
import net.chronoflow.core.sync.SyncPathMonitoring
import net.chronoflow.core.ui.R
import net.chronoflow.feature.trip.page.TripPage
import net.chronoflow.feature.trip.page.TripPageArgs
import net.chronoflow.feature.trip.page.TripPageViewModel
import net.chronoflow.feature.trip.state.TripPageLoadingBar
import net.chronoflow.feature.trip.timeline.TextPaintMetrics
import net.chronoflow.feature.trip.timeline.TimelineTypefaces
import java.time.Instant

@Composable
fun TripPageRoute(
    route: Route.TripPage,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onTrips: (() -> Unit)? = null,
    onToday: (() -> Unit)? = null,
    onRouteChange: (Route.TripPage) -> Unit = {},
    onWizard: (net.chronoflow.core.navigation.WizardMode) -> Unit = {},
    preparer: net.chronoflow.feature.trip.actions.TripWizardPreparer? = null,
) {
    val appContext = LocalContext.current.applicationContext
    val graph = (appContext as ChronoFlowApp).graph
    val session by graph.sessionGraphs.current.collectAsStateWithLifecycle()
    if (session == null) {
        TripPageLoadingBar(onBack, modifier.testTag("route.tripPage"))
        return
    }
    val language = if (LocalConfiguration.current.locales[0].language == "nl") Language.Nl else Language.En
    val summary = LaunchPlatform.startTripAtSummary
    val freeze = LaunchPlatform.freezePlanClock
    val args =
        remember(route.tripId, route.dayId, route.blockId, route.awaitingSync, summary) {
            TripPageArgs(route.tripId, route.dayId, route.blockId, summary, route.awaitingSync)
        }
    val factory =
        remember(args, appContext, language, freeze, preparer) {
            tripPageViewModelFactory(
                args,
                appContext,
                language,
                freeze,
                route.sheet ?: LaunchPlatform.initialTripSheet,
                route.notificationInputHash,
                preparer,
            )
        }
    val vm: TripPageViewModel = viewModel(key = "trip:${route.tripId}", factory = factory)
    val reminders =
        remember(graph, session) {
            net.chronoflow.app.reminders
                .SessionRemindersDependencies(graph, checkNotNull(session))
        }
    TrackTripPageSheet(vm, route, onRouteChange)
    Box(modifier.fillMaxSize().testTag("route.tripPage")) {
        TripPage(
            vm,
            onBack,
            onSettings,
            onTrips = onTrips,
            onToday = onToday,
            onWizard = onWizard,
        )
        net.chronoflow.app.reminders
            .FirstTripRemindersHost(reminders, graph.dispatchers.main, route.tripId)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
internal fun tripPageViewModelFactory(
    args: TripPageArgs,
    context: Context,
    language: Language,
    freezeClock: Boolean,
    initialSheet: net.chronoflow.core.navigation.TripSheet? = null,
    notificationInputHash: String? = null,
    preparer: net.chronoflow.feature.trip.actions.TripWizardPreparer? = null,
) = viewModelFactory {
    initializer {
        val graph = checkNotNull(this[AppGraphKey])
        val session = checkNotNull(graph.sessionGraphs.current.value)
        val appContext = context.applicationContext
        val compute = graph.dispatchers.default
        val buildDispatcher = compute.limitedParallelism(2)
        TripPageViewModel(
            args,
            DbTripPageSource(session),
            if (freezeClock) FixedPlanClock(LaunchPlatform.frozenTripInstant ?: Instant.now()) else graph.planClock,
            session.tripPlanPipeline(compute, graph.copyCatalogs, graph.dispatchers.io),
            createSavedStateHandle(),
            language,
            compute,
            buildDispatcher,
            { TextPaintMetrics(TimelineTypefaces.load(appContext, graph.dispatchers.io)) },
            formatter = { CopyLineFormatter(graph.copyCatalogs.catalog()) },
            refreshProbe = LaunchPlatform.refreshProbe,
            fetchDetail = { id -> session.sync.fetchDetail(id, session.snapshot.session, session.snapshot.epoch) },
            makePath = { ioPath(ConnectivitySyncPathMonitor(appContext), graph.dispatchers.io) },
            syncEvents = session.sync.events(),
            epoch = session.snapshot.epoch,
            initiallySyncCompleted = false,
            initialTimeline = !LaunchPlatform.startTripInList,
            initialSheet = initialSheet,
            notificationInputHash = notificationInputHash,
            preparer = preparer,
            isCurrent = { graph.sessionGraphs.current.value === session },
            enqueueCheck = { change ->
                val permit =
                    graph.controller.mutationPermit(session.snapshot)
                        ?: throw net.chronoflow.core.model.session
                            .StaleSessionException()
                withContext(graph.dispatchers.io) {
                    session.outbox.enqueue(
                        net.chronoflow.core.model.OutboxChange
                            .CheckOff(change),
                        permit = permit,
                    )
                }
            },
            reportCheckFailure = { error ->
                if (net.chronoflow.app.BuildConfig.DEBUG) {
                    android.util.Log.w(
                        "CfTrip",
                        "check-off not saved: ${error::class.simpleName}",
                    )
                }
            },
        )
    }
}

private fun ioPath(
    path: SyncPathMonitoring,
    io: CoroutineDispatcher,
): SyncPathMonitoring =
    object : SyncPathMonitoring {
        override fun updates() = path.updates().flowOn(io)

        override suspend fun stop() = withContext(io) { path.stop() }
    }

@Composable
internal fun TrackTripPageSheet(
    vm: TripPageViewModel,
    route: Route.TripPage,
    onRouteChange: (Route.TripPage) -> Unit,
) {
    val update by rememberUpdatedState(onRouteChange)
    val sheet by vm.sheet.collectAsStateWithLifecycle()
    val resolved by vm.sheetResolved.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(sheet, resolved) {
        if (resolved && route.sheet != sheet) update(route.copy(sheet = sheet))
    }
}
