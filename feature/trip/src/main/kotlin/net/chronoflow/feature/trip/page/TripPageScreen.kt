
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
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
import kotlinx.coroutines.flow.first
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
import net.chronoflow.core.ui.window.LocalWindowSize
import net.chronoflow.feature.trip.list.DayListRow
import net.chronoflow.feature.trip.list.ZoneChangeCard
import net.chronoflow.feature.trip.state.TripPageErrorPage
import net.chronoflow.feature.trip.state.TripPageStates
import net.chronoflow.feature.trip.timeline.DayTimeline
import net.chronoflow.feature.trip.timeline.LocalTimelineJankTagger
import net.chronoflow.feature.trip.timeline.TimelinePresentedMarker
import net.chronoflow.feature.trip.timeline.TimelineResources
import net.chronoflow.feature.trip.timeline.host.timelineDrawingKey
import net.chronoflow.feature.trip.timeline.recoverDrawingFailure
import net.chronoflow.feature.trip.wide.TripPageFrame
import net.chronoflow.feature.trip.wide.TripScrollTracker
import net.chronoflow.feature.trip.wide.WideDayRow
import net.chronoflow.feature.trip.wide.WideListRows
import net.chronoflow.feature.trip.wide.WidePinSource
import kotlin.math.roundToInt

private class PageRestoration {
    var job: Job? = null
    val tracker = TripScrollTracker()
    var previousIndex: PageIndex? = null
    var thresholdPx: Int = 0
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
    val windowWidth =
        LocalWindowSize.current.width.value
            .toDouble()
    val wide = TripPageLayout.resolve(windowWidth).isWide
    val renderedTimeline = state.timeline || wide
    val plan = state.ui.plan
    val listState = state.listState
    val showToggle =
        !wide &&
            when (state.ui.state) {
                TripPageState.Ready, TripPageState.Loading, TripPageState.RemoteLoading -> true
                else -> false
            }
    val dayId = state.anchor.dayID
    val index = if (renderedTimeline) plan?.index else plan?.listIndex
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
                    restoration.tracker.cancelRestoration()
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
    val pin = remember(listState) { WidePinSource(listState) }
    val density = LocalDensity.current
    val safe = LocalSafeInsets.current
    val direction = LocalLayoutDirection.current
    val reduceMotion = LocalReduceMotion.current
    val dark = CfTheme.colors.isDark
    val thresholdPx = with(density) { PlanMetrics.compactHeaderHeight.sp.roundToPx() }
    val fadePx = with(density) { PlanMetrics.compactFadeDistance.dp.toPx() }
    val windowWidth =
        LocalWindowSize.current.width.value
            .toDouble()
    val renderedTimeline = state.timeline || TripPageLayout.resolve(windowWidth).isWide
    val index: PageIndex = if (renderedTimeline) plan.index else plan.listIndex
    val sizeKey =
        TripScrollTracker.SizeKey(
            windowWidth.roundToInt(),
            density.density,
            TripPageLayout.resolve(windowWidth),
            renderedTimeline,
        )
    SideEffect {
        Snapshot.withoutReadObservation {
            val info = listState.layoutInfo
            val samples = info.visibleItemsInfo.map { TripScrollTracker.ItemSample(it.key, it.contentType, it.offset) }
            val restore =
                restoration.tracker.onSize(
                    sizeKey,
                    samples,
                    info.viewportStartOffset,
                    restoration.previousIndex ?: index,
                    index,
                    restoration.thresholdPx.takeIf { it > 0 } ?: thresholdPx,
                    listState.isScrollInProgress,
                )
            restoration.previousIndex = index
            restoration.thresholdPx = thresholdPx
            if (restore != null) {
                listState.requestScrollToItem(restore.index, restore.offsetPx)
                restoration.tracker.lastCaptured?.let { captured ->
                    onEvent(TripPageEvent.Select(captured.dayId, if (captured.onHeader) 0.0 else captured.offsetDp))
                    onEvent(TripPageEvent.SaveAnchor)
                }
            }
        }
    }
    val days = plan.prepared.days
    val anchorId = state.anchor.dayID
    val currentState = rememberUpdatedState(state)
    val currentEvent = rememberUpdatedState(onEvent)

    fun offsetDp(id: String): Double =
        if (!renderedTimeline) {
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
    LaunchedEffect(tagger, anchorId, days, renderedTimeline) {
        val position = days.indexOfFirst { it.id == anchorId }
        if (position >= 0) tagger?.onVisibleDay(position, if (renderedTimeline) "timeline" else "list")
    }
    val resources =
        remember(density.density, dark) {
            TimelineResources.Cache.get(
                density.density,
                dark,
                if (dark) TimelinePalette.Dark else TimelinePalette.Light,
            )
        }
    var focusDay by remember { mutableStateOf<String?>(null) }
    val focusHandled = remember { { focusDay = null } }
    val onCheck =
        remember(onEvent) {
            {
                id: String,
                status: net.chronoflow.core.model.contracts.CheckStatus?,
                ->
                onEvent(TripPageEvent.Check(id, status))
            }
        }
    val clocks = state.clocks
    val selectedBlock = state.selectedBlock
    val onFirstReplay = remember(state.firstFrame) { state.firstFrame::onReplay }
    val onBlock = remember(onEvent) { { id: String -> onEvent(TripPageEvent.Block(id)) } }
    val stickyTop =
        with(density) {
            maxOf(
                (PlanMetrics.compactHeaderHeight.dp * fontScale).roundToPx(),
                (
                    (PlanMetrics.planListTitleLineHeight + PlanMetrics.planLaneLineHeight).dp * fontScale +
                        PlanMetrics.smallGap.dp
                ).roundToPx(),
            )
        }
    BoxWithConstraints(
        Modifier.fillMaxSize().testTag("trip.page").layout { measurable, constraints ->
            pin.stickyTopPx = stickyTop
            pin.bottomInsetPx = safe.getBottom(this)
            pin.maxHeightPx = PlanMetrics.wideListMaxHeight.dp.roundToPx()
            val child = measurable.measure(constraints)
            layout(child.width, child.height) { child.placeRelative(0, 0) }
        },
    ) {
        val width = maxWidth.value.toDouble()
        val left = with(density) { safe.getLeft(this, direction).toDp() }
        val right = with(density) { safe.getRight(this, direction).toDp() }
        val bottom = with(density) { safe.getBottom(this).toDp() }
        val frame =
            remember(windowWidth, width, left, right) {
                TripPageFrame.resolve(windowWidth, width - left.value - right.value)
            }
        val gutter = frame.gutterDp.dp
        val available = frame.timelineWidthDp
        val compact = frame.compact
        val geometry = rememberUpdatedState(Triple(available, compact, density))
        val appearance = rememberUpdatedState(dark to reduceMotion)
        LaunchedEffect(state.commands, listState) {
            state.commands.collect { command ->
                restoration.tracker.cancelRestoration()
                restoration.job?.cancel()
                restoration.job =
                    launch {
                        if (!currentState.value.timeline && !frame.layout.isWide) {
                            val replacement =
                                snapshotFlow { currentState.value.ui.plan }.first {
                                    it?.layouts?.containsKey(command.dayId) == true
                                } ?: return@launch
                            listState.scrollToItem(replacement.listIndex.headerIndex(command.dayId), 0)
                            if (command.animated) focusDay = command.dayId
                            return@launch
                        }
                        val latest = currentState.value.ui.plan ?: return@launch
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
                        if (command.animated) focusDay = command.dayId
                    }
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().testTag("trip.scroll"),
            contentPadding =
                PaddingValues(
                    start = gutter + left,
                    end = gutter + right,
                    bottom =
                        PlanMetrics.gutter.dp + bottom,
                ),
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
                    ) { DayHeader(day, requestFocus = focusDay == day.id, onFocusRequest = focusHandled) }
                }
                if (renderedTimeline) {
                    item("t:${day.id}", "dayTimeline") {
                        WideDayRow(
                            day.id,
                            frame,
                            timeline = {
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
                                    actions = day.checkActions,
                                    onCheck = onCheck,
                                    headings = { LaneHeadings(null) },
                                )
                            },
                            list =
                                if (frame.layout.isWide && plan.layouts.containsKey(day.id)) {
                                    {
                                        WideListRows(
                                            day,
                                            plan.listRows[position],
                                            clocks.clock(day.id),
                                            selectedBlock,
                                            pin,
                                            onBlock,
                                            onCheck,
                                        )
                                    }
                                } else {
                                    null
                                },
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
                                DayListRow(
                                    row,
                                    state.clocks.clock(day.id),
                                    onBlock,
                                    rowPadding,
                                    actions =
                                        day.checkActions.byBlock[row.blockId]
                                            ?: net.chronoflow.core.plan.step.StepActions.None,
                                    onCheck = onCheck,
                                )
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
            timeline = renderedTimeline,
            wide = frame.takeIf { it.layout.isWide },
        )
        if (!renderedTimeline) {
            Box(Modifier.size(PlanMetrics.border.dp).testTag("trip.timeline.ready"))
        }
        Box(
            Modifier
                .size(
                    PlanMetrics.border.dp,
                ).testTag(if (renderedTimeline) "trip.mode.timeline" else "trip.mode.list"),
        )
        TimelinePresentedMarker(state.firstFrame.presented, Modifier.align(Alignment.TopStart))
    }
}
