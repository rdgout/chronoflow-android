package net.chronoflow.app.trip

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
        remember(args, appContext, language, freeze) { tripPageViewModelFactory(args, appContext, language, freeze) }
    Box(modifier.fillMaxSize().testTag("route.tripPage")) {
        TripPage(
            viewModel(key = "trip:${route.tripId}", factory = factory),
            onBack,
            onSettings,
            onTrips = onTrips,
            onToday = onToday,
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
internal fun tripPageViewModelFactory(
    args: TripPageArgs,
    context: Context,
    language: Language,
    freezeClock: Boolean,
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
            if (freezeClock) FixedPlanClock(Instant.now()) else graph.planClock,
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
