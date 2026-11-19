package net.chronoflow.feature.trip.page

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.plan.drawing.DayDrawingKey
import net.chronoflow.core.plan.trippage.TripPageAnchor
import net.chronoflow.core.plan.trippage.TripPageState
import net.chronoflow.core.sync.SyncResult
import net.chronoflow.core.sync.refresh.RefreshDebugProbe
import net.chronoflow.core.sync.refresh.RefreshFeedbackState
import net.chronoflow.feature.trip.timeline.DayClocks
import net.chronoflow.feature.trip.timeline.TimelineDrawingRepository
import net.chronoflow.feature.trip.timeline.host.FirstTimelineFrameTrace

sealed interface TripPageEvent {
    data object Back : TripPageEvent

    data object Settings : TripPageEvent

    data object Legend : TripPageEvent

    data object Actions : TripPageEvent

    data object Refresh : TripPageEvent

    data object ToggleView : TripPageEvent

    data object SaveAnchor : TripPageEvent

    data class DismissFeedback(
        val id: Long,
    ) : TripPageEvent

    data class Jump(
        val dayId: String,
    ) : TripPageEvent

    data class Select(
        val dayId: String,
        val offsetDp: Double,
    ) : TripPageEvent

    data class Block(
        val id: String,
    ) : TripPageEvent
}

class TripPageScreenState(
    val ui: TripPageUi,
    val anchor: TripPageAnchor,
    val listState: LazyListState?,
    val clocks: DayClocks,
    val summaryClock: SummaryClockState,
    val repository: TimelineDrawingRepository,
    val firstFrame: FirstTimelineFrameTrace,
    val commands: Flow<ScrollCommand>,
    val prefetch: (Int, DayDrawingKey) -> Unit,
    val refresh: RefreshFeedbackState,
    val refreshProbe: RefreshDebugProbe,
    val canSettings: Boolean,
    val canLegend: Boolean,
    val canActions: Boolean,
    val timeline: Boolean = true,
)

@Suppress("ktlint:compose:vm-forwarding-check")
@Composable
fun TripPage(
    viewModel: TripPageViewModel,
    onBack: () -> Unit,
    onSettings: (() -> Unit)?,
    onLegend: (() -> Unit)? = null,
    onActions: (() -> Unit)? = null,
    onRefreshResult: (SyncResult?) -> Unit = {},
) {
    val state = rememberTripPageScreenState(viewModel, onSettings != null, onLegend != null, onActions != null)
    val scope = rememberCoroutineScope()
    val back = rememberUpdatedState(onBack)
    val settings = rememberUpdatedState(onSettings)
    val legend = rememberUpdatedState(onLegend)
    val actions = rememberUpdatedState(onActions)
    val refreshResult = rememberUpdatedState(onRefreshResult)
    val onEvent: (TripPageEvent) -> Unit =
        remember(viewModel, scope) {
            { event ->
                when (event) {
                    TripPageEvent.Back -> {
                        back.value()
                    }

                    TripPageEvent.Settings -> {
                        settings.value?.invoke()
                    }

                    TripPageEvent.Legend -> {
                        legend.value?.invoke()
                    }

                    TripPageEvent.Actions -> {
                        actions.value?.invoke()
                    }

                    TripPageEvent.Refresh -> {
                        scope.launch { refreshResult.value(viewModel.refresh()) }
                    }

                    TripPageEvent.ToggleView -> {
                        viewModel.toggleView()
                    }

                    TripPageEvent.SaveAnchor -> {
                        viewModel.saveAnchor()
                    }

                    is TripPageEvent.DismissFeedback -> {
                        viewModel.dismissFeedback(event.id)
                    }

                    is TripPageEvent.Jump -> {
                        viewModel.jump(event.dayId)
                    }

                    is TripPageEvent.Select -> {
                        viewModel.select(event.dayId, event.offsetDp)
                    }

                    is TripPageEvent.Block -> {
                        viewModel.selectBlock(event.id)
                    }
                }
            }
        }
    TripPageScreen(state, onEvent)
}

@Composable
internal fun rememberTripPageScreenState(
    viewModel: TripPageViewModel,
    canSettings: Boolean = false,
    canLegend: Boolean = false,
    canActions: Boolean = false,
): TripPageScreenState {
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.runClock() }
    }
    val language = if (LocalConfiguration.current.locales[0].language == "nl") Language.Nl else Language.En
    LaunchedEffect(viewModel, language) { viewModel.setLanguage(language) }
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val anchor by viewModel.anchor.collectAsStateWithLifecycle()
    val refresh by viewModel.refreshState.collectAsStateWithLifecycle()
    val timeline by viewModel.timeline.collectAsStateWithLifecycle()
    val density = LocalDensity.current
    val listState =
        if (ui.state == TripPageState.Ready && ui.plan != null) {
            remember(viewModel) {
                val opening = viewModel.openingScroll() ?: TripPageAnchor()
                val target =
                    if (viewModel.consumeSummaryPosition()) {
                        0 to 0
                    } else if (!timeline) {
                        (opening.dayID?.let { ui.plan!!.listIndex.headerIndex(it) } ?: 0) to 0
                    } else {
                        restoreTarget(ui.plan!!.index, opening, with(density) { opening.offset.dp.roundToPx() })
                    }
                LazyListState(target.first, target.second)
            }
        } else {
            null
        }
    val prefetch = remember(viewModel) { viewModel::prefetchAround }
    return TripPageScreenState(
        ui,
        anchor,
        listState,
        viewModel.clocks,
        viewModel.summaryClock,
        viewModel.repository,
        viewModel.firstFrame,
        viewModel.scrollCommands,
        prefetch,
        refresh,
        viewModel.refreshProbe,
        canSettings,
        canLegend,
        canActions,
        timeline,
    )
}
