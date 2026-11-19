
package net.chronoflow.feature.trip.page

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import net.chronoflow.core.designsystem.layout.LocalSafeInsets
import net.chronoflow.core.designsystem.system.CfPullToRefreshBox
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.designsystem.theme.LocalReduceMotion
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.plan.generated.TimelinePalette
import net.chronoflow.core.plan.trippage.TripPageAnchor
import net.chronoflow.core.plan.trippage.TripPageLayout
import net.chronoflow.core.plan.trippage.TripPageState
import net.chronoflow.core.ui.R
import net.chronoflow.core.ui.refresh.RefreshFeedbackCapsule
import net.chronoflow.feature.trip.list.DayListRow
import net.chronoflow.feature.trip.list.ZoneChangeCard
import net.chronoflow.feature.trip.state.TripPageErrorPage
import net.chronoflow.feature.trip.state.TripPageStates
import net.chronoflow.feature.trip.timeline.DayTimeline
import net.chronoflow.feature.trip.timeline.LocalTimelineJankTagger
import net.chronoflow.feature.trip.timeline.TimelinePresentedMarker
import net.chronoflow.feature.trip.timeline.TimelineResources
import net.chronoflow.feature.trip.timeline.host.timelineAvailableWidthDp
import net.chronoflow.feature.trip.timeline.host.timelineDrawingKey
import net.chronoflow.feature.trip.timeline.recoverDrawingFailure

private class PageRestoration {
    var job: Job? = null
}

@Composable
fun TripPageScreen(
    state: TripPageScreenState,
    onEvent: (TripPageEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.ui.state == TripPageState.NotFound || state.ui.state == TripPageState.Server) {
        Box(modifier.fillMaxSize()) {
            TripPageErrorPage(
                state.ui.state,
                state.refresh.refreshing,
                { onEvent(TripPageEvent.Refresh) },
                { onEvent(TripPageEvent.Back) },
                { onEvent(if (state.ui.state == TripPageState.NotFound) TripPageEvent.Trips else TripPageEvent.Retry) },
                if (state.canToday) ({ onEvent(TripPageEvent.Today) }) else null,
            )
            RefreshFeedbackCapsule(state.refresh, {
                onEvent(TripPageEvent.DismissFeedback(it))
            }, state.refreshProbe, Modifier.align(Alignment.TopCenter))
        }
        return
    }
    val plan = state.ui.plan
    val listState = state.listState
    val showToggle =
        when (state.ui.state) {
            TripPageState.Ready, TripPageState.Loading, TripPageState.RemoteLoading -> true
            else -> false
        }
    val dayId = state.anchor.dayID
    val index = if (state.timeline) plan?.index else plan?.listIndex
    val restoration = remember(listState) { PageRestoration() }
    val header = if (dayId != null && plan?.layouts?.containsKey(dayId) == true) index?.headerIndex(dayId) else null
    var scrolled by remember(listState, header) {
        mutableStateOf(
            listState != null && header != null &&
                Snapshot.withoutReadObservation {
                    headerScrolled(listState.layoutInfo.visibleItemsInfo, listState.firstVisibleItemIndex, header)
                },
        )
    }
    // Composition-read derived state over layoutInfo still schedules passes when its value stays unchanged.
    LaunchedEffect(listState, header) {
        if (listState != null && header != null) {
            snapshotFlow {
                headerScrolled(listState.layoutInfo.visibleItemsInfo, listState.firstVisibleItemIndex, header)
            }.collect { scrolled = it }
        }
    }
    val anchorDay = remember(plan, dayId) { plan?.prepared?.days?.firstOrNull { it.id == dayId } }
    Scaffold(
        modifier = modifier,
        topBar = {
            TripTopBar(
                state.ui.title,
                plan?.prepared,
                listState != null,
                anchorDay,
                scrolled,
                {
                    onEvent(TripPageEvent.Back)
                },
                { position ->
                    plan
                        ?.prepared
                        ?.days
                        ?.getOrNull(position)
                        ?.let { onEvent(TripPageEvent.Jump(it.id)) }
                },
                if (state.canActions) ({ onEvent(TripPageEvent.Actions) }) else null,
                timeline = state.timeline,
                showToggle = showToggle,
                toggleEnabled = state.ui.state == TripPageState.Ready && plan != null,
                onToggle = {
                    restoration.job?.cancel()
                    onEvent(TripPageEvent.ToggleView)
                    if (dayId != null && plan != null && listState != null) {
                        val next: PageIndex = if (state.timeline) plan.listIndex else plan.index
                        listState.requestScrollToItem(next.headerIndex(dayId), 0)
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = CfTheme.colors.background,
    ) { inner ->
        if (state.ui.state != TripPageState.Ready || plan == null || listState == null) {
            CfPullToRefreshBox(state.refresh.refreshing, {
                onEvent(TripPageEvent.Refresh)
            }, Modifier.fillMaxSize().padding(inner)) {
                TripPageStates(state, onEvent)
                RefreshFeedbackCapsule(state.refresh, {
                    onEvent(TripPageEvent.DismissFeedback(it))
                }, state.refreshProbe, Modifier.align(Alignment.TopCenter))
            }
        } else {
            CfPullToRefreshBox(state.refresh.refreshing, {
                onEvent(TripPageEvent.Refresh)
            }, Modifier.fillMaxSize().padding(inner)) {
                ReadyTripPage(state, plan, restoration, onEvent)
                RefreshFeedbackCapsule(
                    state.refresh,
                    { onEvent(TripPageEvent.DismissFeedback(it)) },
                    state.refreshProbe,
                    Modifier.align(Alignment.TopCenter),
                )
            }
        }
    }
}

@Composable
private fun ReadyTripPage(
    state: TripPageScreenState,
    plan: ReadyPlan,
    restoration: PageRestoration,
    onEvent: (TripPageEvent) -> Unit,
) {
    val listState = checkNotNull(state.listState)
    val density = LocalDensity.current
    val safe = LocalSafeInsets.current
    val direction = LocalLayoutDirection.current
    val reduceMotion = LocalReduceMotion.current
    val dark = CfTheme.colors.isDark
    val thresholdPx = with(density) { PlanMetrics.compactHeaderHeight.sp.roundToPx() }
    val fadePx = with(density) { PlanMetrics.compactFadeDistance.dp.toPx() }
    val index: PageIndex = if (state.timeline) plan.index else plan.listIndex
    val days = plan.prepared.days
    val anchorId = state.anchor.dayID
    val currentState = rememberUpdatedState(state)
    val currentEvent = rememberUpdatedState(onEvent)

    fun offsetDp(id: String): Double =
        if (!state.timeline) {
            0.0
        } else {
            with(density) {
                val px = offsetInDayPx(listState.layoutInfo.visibleItemsInfo, plan.index.timelineIndex(id))
                px.toDp().value.toDouble()
            }
        }
    LaunchedEffect(listState, index, thresholdPx, density) {
        snapshotFlow { currentDay(listState.layoutInfo.visibleItemsInfo, index, thresholdPx) }.collect { id ->
            id?.let { currentEvent.value(TripPageEvent.Select(it, offsetDp(it))) }
        }
    }
    LaunchedEffect(listState, index, density) {
        snapshotFlow { listState.isScrollInProgress }.filter { !it }.collect {
            val id = currentState.value.anchor.dayID
            if (id != null && plan.layouts.containsKey(id)) {
                currentEvent.value(TripPageEvent.Select(id, offsetDp(id)))
            }
            currentEvent.value(TripPageEvent.SaveAnchor)
        }
    }
    val tagger = LocalTimelineJankTagger.current
    LaunchedEffect(tagger, anchorId, days, state.timeline) {
        val position = days.indexOfFirst { it.id == anchorId }
        if (position >= 0) tagger?.onVisibleDay(position, if (state.timeline) "timeline" else "list")
    }
    val resources =
        remember(density.density, dark) {
            TimelineResources.Cache.get(
                density.density,
                dark,
                if (dark) TimelinePalette.Dark else TimelinePalette.Light,
            )
        }
    val onFirstReplay = remember(state.firstFrame) { state.firstFrame::onReplay }
    val onBlock = remember(onEvent) { { id: String -> onEvent(TripPageEvent.Block(id)) } }
    BoxWithConstraints(Modifier.fillMaxSize().testTag("trip.page")) {
        val width = maxWidth.value.toDouble()
        val left = with(density) { safe.getLeft(this, direction).toDp() }
        val right = with(density) { safe.getRight(this, direction).toDp() }
        val bottom = with(density) { safe.getBottom(this).toDp() }
        val gutter = TripPageLayout.resolve(width).gutter.dp
        val available = timelineAvailableWidthDp(width, left.value.toDouble(), right.value.toDouble())
        val compact = width < PlanMetrics.timelineLabelBreakpoint
        val geometry = rememberUpdatedState(Triple(available, compact, density))
        val appearance = rememberUpdatedState(dark to reduceMotion)
        LaunchedEffect(state.commands, listState) {
            state.commands.collect { command ->
                restoration.job?.cancel()
                restoration.job =
                    launch {
                        val latest = currentState.value.ui.plan ?: return@launch
                        if (!currentState.value.timeline) {
                            listState.requestScrollToItem(latest.listIndex.headerIndex(command.dayId), 0)
                            return@launch
                        }
                        val day = latest.prepared.days.firstOrNull { it.id == command.dayId } ?: return@launch
                        val (availableWidth, compactWidth, currentDensity) = geometry.value
                        val (isDark, instant) = appearance.value
                        val key =
                            timelineDrawingKey(
                                day.id,
                                latest.prepared.revision,
                                availableWidth,
                                currentDensity,
                                isDark,
                                compactWidth,
                                latest.language,
                            )
                        recoverDrawingFailure({ android.util.Log.w("CfTrip", "drawing preparation failed") }) {
                            withTimeoutOrNull(50) {
                                state.repository.drawing(key, day, latest.layouts.getValue(day.id))
                            }
                        }
                        val target =
                            restoreTarget(
                                latest.index,
                                TripPageAnchor(command.dayId),
                                with(currentDensity) { command.offset.dp.roundToPx() },
                            )
                        if (command.animated && !instant) {
                            listState.animateScrollToItem(target.first, target.second)
                        } else {
                            listState.scrollToItem(target.first, target.second)
                        }
                    }
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().testTag("trip.scroll"),
            contentPadding = PaddingValues(start = gutter + left, end = gutter + right, bottom = gutter + bottom),
        ) {
            item("summary", "summary") {
                SummaryCard(
                    plan.prepared,
                    state.summaryClock,
                    if (state.canLegend) ({ onEvent(TripPageEvent.Legend) }) else null,
                )
            }
            item("days", "dayNav") {
                DayNavigation(
                    days,
                    anchorId,
                    { onEvent(TripPageEvent.Jump(it)) },
                    Modifier.padding(top = PlanMetrics.sectionGap.dp),
                )
            }
            days.forEachIndexed { position, day ->
                item("h:${day.id}", "dayHeader") {
                    Box(
                        Modifier.testTag("trip.headerItem.${day.id}").padding(top = PlanMetrics.sectionGap.dp),
                    ) { DayHeader(day) }
                }
                if (state.timeline) {
                    item("t:${day.id}", "dayTimeline") {
                        DayTimeline(
                            day,
                            plan.layouts.getValue(day.id),
                            timelineDrawingKey(
                                day.id,
                                plan.prepared.revision,
                                available,
                                density,
                                dark,
                                compact,
                                plan.language,
                            ),
                            position,
                            available,
                            state.repository,
                            resources,
                            state.clocks.clock(day.id),
                            onBlock,
                            onFirstReplay,
                            state.prefetch,
                            headings = { LaneHeadings(null) },
                        )
                    }
                } else {
                    plan.listRows[position].rows.forEachIndexed { rowIndex, row ->
                        item(row.key, if (row.zone) "zoneRow" else "listRow") {
                            val gap = if (rowIndex == 0) 0.dp else PlanMetrics.rowGap.dp
                            val rowPadding = Modifier.padding(top = gap)
                            if (row.zone) {
                                ZoneChangeCard(row, onBlock, rowPadding)
                            } else {
                                DayListRow(row, state.clocks.clock(day.id), onBlock, rowPadding)
                            }
                        }
                    }
                }
            }
            item("end", "end") {
                EndOfPlanCard(
                    plan.prepared,
                    { onEvent(TripPageEvent.Jump(it)) },
                    Modifier.padding(top = PlanMetrics.sectionGap.dp),
                )
            }
            if (index.hidden != null) {
                item("hidden", "hidden") {
                    HiddenStepsButton(
                        if (state.canSettings) ({ onEvent(TripPageEvent.Settings) }) else null,
                        Modifier.padding(top = PlanMetrics.sectionGap.dp),
                    )
                }
            }
        }
        val anchorHeader = anchorId?.takeIf { plan.layouts.containsKey(it) }?.let(index::headerIndex)
        val alpha =
            remember(listState, anchorHeader, fadePx, reduceMotion) {
                {
                    if (anchorHeader == null) {
                        0f
                    } else {
                        compactAlpha(
                            headerBottomPx(
                                listState.layoutInfo.visibleItemsInfo,
                                listState.firstVisibleItemIndex,
                                anchorHeader,
                            ),
                            fadePx,
                            reduceMotion,
                        )
                    }
                }
            }
        var visible by remember(alpha) {
            mutableStateOf(Snapshot.withoutReadObservation { alpha() >= 1f })
        }
        LaunchedEffect(alpha) {
            snapshotFlow { alpha() >= 1f }.collect { visible = it }
        }
        val day = remember(days, anchorId) { days.firstOrNull { it.id == anchorId } }
        CompactHeader(
            day,
            alpha,
            visible,
            PaddingValues(start = gutter + left, end = gutter + right),
            Modifier.align(Alignment.TopStart),
            timeline = state.timeline,
        )
        if (!state.timeline) {
            Box(Modifier.size(PlanMetrics.border.dp).testTag("trip.timeline.ready"))
        }
        TimelinePresentedMarker(state.firstFrame.presented, Modifier.align(Alignment.TopStart))
    }
}
