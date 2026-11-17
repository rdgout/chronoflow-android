package net.chronoflow.feature.trip.timeline

import android.animation.ValueAnimator
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import net.chronoflow.core.designsystem.generated.CfColors
import net.chronoflow.core.designsystem.layout.LocalSafeInsets
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.plan.generated.TimelinePalette
import net.chronoflow.core.plan.trippage.TripPageLayout
import net.chronoflow.core.ui.R
import net.chronoflow.feature.trip.timeline.host.TimelineHostModel
import net.chronoflow.feature.trip.timeline.host.TimelineHostState
import net.chronoflow.feature.trip.timeline.host.TimelineHostViewModel
import net.chronoflow.feature.trip.timeline.host.TimelineListIndex
import net.chronoflow.feature.trip.timeline.host.timelineAvailableWidthDp
import net.chronoflow.feature.trip.timeline.host.timelineDrawingKey

/**
 * The 1d host: a lazy list of keyed days with a plain sticky header, a minimal day menu and a plain end item, without
 * polish. Phase 6b replaces the chrome and the data source and keeps `DayTimeline`, `NowLine`, the clock and the probe.
 */
@Suppress("ktlint:compose:vm-forwarding-check")
@Composable
fun TimelinePage(
    viewModel: TimelineHostViewModel,
    onBlock: (String) -> Unit,
    modifier: Modifier = Modifier,
    onListState: ((LazyListState) -> Unit)? = null,
) {
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.runClock() }
    }
    val colors = timelineColors()
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val current = state) {
        TimelineHostState.Preparing -> {
            val loading = stringResource(R.string.plan_loading_your_plan)
            Box(
                modifier
                    .fillMaxSize()
                    .background(colors.background)
                    .semantics { contentDescription = loading },
            )
        }

        is TimelineHostState.Ready -> {
            ReadyPage(viewModel, current.model, colors, onBlock, onListState, modifier)
        }
    }
}

@Suppress("ktlint:compose:vm-forwarding-check")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ReadyPage(
    viewModel: TimelineHostViewModel,
    model: TimelineHostModel,
    colors: CfColors,
    onBlock: (String) -> Unit,
    onListState: ((LazyListState) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val safe = LocalSafeInsets.current
    val layoutDirection = LocalLayoutDirection.current
    val index = model.index
    val days = model.prepared.days
    val thresholdPx = with(density) { PlanMetrics.compactHeaderHeight.sp.roundToPx() }
    val thresholdDp = with(density) { PlanMetrics.compactHeaderHeight.sp.toDp() }
    val reduceMotion = !ValueAnimator.areAnimatorsEnabled()
    val dark = colors.isDark
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = index.headerIndex(model.openingDayId))
    val resources =
        remember(density.density, dark) {
            TimelineResources.Cache.get(
                density.density,
                dark,
                if (dark) TimelinePalette.Dark else TimelinePalette.Light,
            )
        }
    val onFirstReplay = remember(viewModel) { viewModel.firstFrame::onReplay }
    val prefetch = remember(viewModel) { viewModel::prefetchAround }
    val scope = rememberCoroutineScope()
    LaunchedEffect(listState) { onListState?.invoke(listState) }
    val tagger = LocalTimelineJankTagger.current
    if (tagger != null) {
        val dayPositions =
            remember(model.prepared) {
                days.withIndex().associate { (position, day) ->
                    day.id to position
                }
            }
        LaunchedEffect(viewModel, tagger, dayPositions) {
            viewModel.anchorDayId.filterNotNull().collect { id ->
                dayPositions[id]?.let { tagger.onVisibleDay(it, "timeline") }
            }
        }
    }
    val scrolled = remember(listState, index) { mutableStateOf(false) }
    val isScrolled = remember(scrolled) { { scrolled.value } }
    LaunchedEffect(listState, index, thresholdPx) {
        snapshotFlow {
            val visible = listState.layoutInfo.visibleItemsInfo
            val position = index.currentDayIndex(visible, thresholdPx)
            val id = position?.let { days[it].id } ?: viewModel.anchorDayId.value
            val pastThreshold = id != null && visible.any { it.index == index.dayIndex(id) && it.offset < thresholdPx }
            position to pastThreshold
        }.distinctUntilChanged()
            .collect { (position, pastThreshold) ->
                scrolled.value = pastThreshold
                if (position != null) viewModel.select(days[position].id)
            }
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val windowWidthDp = maxWidth.value.toDouble()
        val left = with(density) { safe.getLeft(this, layoutDirection).toDp() }
        val right = with(density) { safe.getRight(this, layoutDirection).toDp() }
        val bottom = with(density) { safe.getBottom(this).toDp() }
        val gutter = TripPageLayout.resolve(windowWidthDp).gutter.dp
        val availableWidthDp = timelineAvailableWidthDp(windowWidthDp, left.value.toDouble(), right.value.toDouble())
        val compact = windowWidthDp < PlanMetrics.timelineLabelBreakpoint
        val jump =
            remember(viewModel, model, availableWidthDp, compact, dark, density, reduceMotion, listState) {
                { position: Int ->
                    scope.launch {
                        val day = days[position]
                        viewModel.select(day.id)
                        val key =
                            timelineDrawingKey(
                                day.id,
                                model.prepared.revision,
                                availableWidthDp,
                                density,
                                dark,
                                compact,
                                viewModel.language,
                            )
                        recoverDrawingFailure({ Log.w(LOG_TAG, "drawing failed for ${day.id}", it) }) {
                            withTimeoutOrNull(JUMP_DRAWING_WAIT_MS) {
                                viewModel.repository.drawing(key, day, model.layouts.getValue(day.id))
                            }
                        }
                        val target = index.headerIndex(day.id)
                        if (reduceMotion) listState.scrollToItem(target) else listState.animateScrollToItem(target)
                    }
                    Unit
                }
            }
        Scaffold(
            containerColor = colors.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    title = { DayMenu(viewModel, model, isScrolled, colors, jump) },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = colors.background,
                            titleContentColor = colors.foreground,
                        ),
                    windowInsets = safe.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                )
            },
        ) { innerPadding ->
            Box(Modifier.fillMaxSize().padding(innerPadding).testTag("trip.page")) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().testTag("trip.scroll"),
                    contentPadding =
                        PaddingValues(
                            start = gutter + left,
                            end = gutter + right,
                            bottom = bottom + PlanMetrics.gutter.dp,
                        ),
                ) {
                    for ((dayIndex, day) in days.withIndex()) {
                        stickyHeader(
                            key = TimelineListIndex.headerKey(day.id),
                            contentType = TimelineListIndex.HEADER_CONTENT_TYPE,
                        ) {
                            DayHeaderLine(day, colors, thresholdDp)
                        }
                        item(key = day.id, contentType = TimelineListIndex.DAY_CONTENT_TYPE) {
                            DayTimeline(
                                day = day,
                                layout = model.layouts.getValue(day.id),
                                key =
                                    timelineDrawingKey(
                                        day.id,
                                        model.prepared.revision,
                                        availableWidthDp,
                                        density,
                                        dark,
                                        compact,
                                        viewModel.language,
                                    ),
                                dayIndex = dayIndex,
                                availableWidthDp = availableWidthDp,
                                repository = viewModel.repository,
                                resources = resources,
                                clock = viewModel.clocks.clock(day.id),
                                onBlock = onBlock,
                                onFirstReplay = onFirstReplay,
                                prefetch = prefetch,
                                modifier = Modifier.padding(bottom = PlanMetrics.sectionGap.dp),
                            )
                        }
                    }
                    item(key = TimelineListIndex.END_KEY, contentType = TimelineListIndex.END_CONTENT_TYPE) {
                        Text(
                            text = stringResource(R.string.plan_end_of_this_plan),
                            style = TimelineStyles.planListTitle,
                            color = colors.foreground,
                            modifier = Modifier.testTag("trip.plan.end"),
                        )
                    }
                }
                TimelinePresentedMarker(viewModel.firstFrame.presented, Modifier.align(Alignment.TopStart))
            }
        }
    }
}

private const val JUMP_DRAWING_WAIT_MS = 50L
private const val LOG_TAG = "CfTimeline"
