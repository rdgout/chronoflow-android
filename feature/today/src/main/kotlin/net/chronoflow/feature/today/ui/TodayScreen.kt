@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.feature.today.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.layout.LocalSafeInsets
import net.chronoflow.core.designsystem.layout.WindowPolicy
import net.chronoflow.core.designsystem.system.CfMediumTopAppBar
import net.chronoflow.core.designsystem.system.CfPullToRefreshBox
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.sync.refresh.RefreshDebugProbe
import net.chronoflow.core.sync.refresh.RefreshFeedbackState
import net.chronoflow.core.ui.R
import net.chronoflow.core.ui.clock.MinuteClock
import net.chronoflow.core.ui.refresh.RefreshFeedbackCapsule
import net.chronoflow.core.ui.shell.LocalTabReselect
import net.chronoflow.core.ui.trip.LocalTripCardProbe
import net.chronoflow.core.ui.trip.TripCard
import net.chronoflow.core.ui.trip.TripCardProbe
import net.chronoflow.feature.today.HiddenNotice
import net.chronoflow.feature.today.TodayActions
import net.chronoflow.feature.today.TodayPanelUi
import net.chronoflow.feature.today.TodayUiState

@Composable
fun TodayScreen(
    state: TodayUiState,
    clock: MinuteClock,
    language: Language,
    formatter: CopyLineFormatter?,
    timeFormat: TimeFormat,
    actions: TodayActions,
    onOpenStep: (String) -> Unit,
    modifier: Modifier = Modifier,
    refreshState: RefreshFeedbackState = RefreshFeedbackState(),
    onRefresh: () -> Unit = {},
    onDismissFeedback: (Long) -> Unit = {},
    refreshProbe: RefreshDebugProbe = RefreshDebugProbe.None,
    listState: LazyListState = rememberLazyListState(),
) {
    val todayProbe = LocalTodayProbe.current
    androidx.compose.runtime.SideEffect { todayProbe?.composed(TodayProbe.Kind.SCREEN, "today") }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val signal = LocalTabReselect.current
    var handled by rememberSaveable { mutableIntStateOf(signal) }
    val reduceMotion by rememberUpdatedState(CfTheme.reduceMotion)
    LaunchedEffect(signal) {
        if (signal != handled) {
            handled = signal
            if (reduceMotion) listState.scrollToItem(0) else listState.animateScrollToItem(0)
            scroll.state.heightOffset = 0f
            scroll.state.contentOffset = 0f
        }
    }
    val safe = LocalSafeInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom).asPaddingValues()
    val direction = LocalLayoutDirection.current
    val padding =
        PaddingValues(
            start = PlanUITokens.space4.dp + safe.calculateStartPadding(direction),
            end = PlanUITokens.space4.dp + safe.calculateEndPadding(direction),
            top = PlanUITokens.space4.dp,
            bottom = PlanUITokens.space4.dp + safe.calculateBottomPadding(),
        )
    val itemModifier = Modifier.widthIn(max = WindowPolicy.listCardMaxWidthDp.dp).fillMaxWidth()
    val probe = LocalTodayProbe.current
    val cardProbe =
        remember(probe) {
            probe?.let { today ->
                TripCardProbe { id ->
                    if (id.startsWith("eyebrow.")) {
                        val tripId = id.removePrefix("eyebrow.")
                        today.composed(TodayProbe.Kind.LEAF, "next.$tripId")
                    }
                }
            }
        }
    CompositionLocalProvider(LocalTripCardProbe provides cardProbe) {
        Scaffold(
            modifier.fillMaxSize().nestedScroll(scroll.nestedScrollConnection),
            containerColor = CfTheme.colors.background,
            contentWindowInsets = WindowInsets(0),
            topBar = {
                CfMediumTopAppBar(stringResource(R.string.screens_today), scroll, Modifier.testTag("today.appbar"))
            },
        ) { scaffold ->
            CfPullToRefreshBox(refreshState.refreshing, onRefresh, Modifier.fillMaxSize().padding(scaffold)) {
                LazyColumn(
                    Modifier.fillMaxSize().testTag("today.screen"),
                    state = listState,
                    contentPadding = padding,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    when (state) {
                        TodayUiState.Loading -> {
                            item("loading", contentType = "loading") { PlanLoadingView(itemModifier) }
                        }

                        TodayUiState.Empty -> {
                            item("empty", contentType = "empty") { TodayEmptyCard(actions.create, itemModifier) }
                        }

                        is TodayUiState.Completed -> {
                            item("completed", contentType = "completed") {
                                TodayCompletedCard(
                                    state,
                                    clock,
                                    language,
                                    checkNotNull(formatter),
                                    actions.create,
                                    actions.pastTrips,
                                    itemModifier,
                                )
                            }
                        }

                        is TodayUiState.Future -> {
                            item("hero", contentType = "hero") {
                                val todayProbe = LocalTodayProbe.current
                                androidx.compose.runtime.SideEffect {
                                    todayProbe?.composed(
                                        TodayProbe.Kind.HERO,
                                        state.hero.card.id,
                                    )
                                }
                                Box(itemModifier.testTag("today.trip")) {
                                    TodayFutureTripCard(state, clock, language, checkNotNull(formatter), {
                                        actions.openTrip(state.hero.card.id)
                                    }, Modifier.fillMaxWidth())
                                }
                            }
                            item("note", contentType = "note") {
                                TodayFutureNote(
                                    state,
                                    language,
                                    checkNotNull(formatter),
                                    itemModifier.padding(top = PlanUITokens.space6.dp),
                                )
                            }
                        }

                        is TodayUiState.Active -> {
                            item("hero", contentType = "hero") {
                                val todayProbe = LocalTodayProbe.current
                                androidx.compose.runtime.SideEffect {
                                    todayProbe?.composed(
                                        TodayProbe.Kind.HERO,
                                        state.hero.id,
                                    )
                                }
                                Box(itemModifier.testTag("today.trip")) {
                                    TripCard(state.hero, {
                                        actions.openTrip(state.hero.id)
                                    }, clock, checkNotNull(formatter), Modifier.fillMaxWidth(), language)
                                }
                            }
                            state.panel?.let { panel ->
                                todayPanel(
                                    panel,
                                    itemModifier,
                                    clock,
                                    language,
                                    checkNotNull(formatter),
                                    onOpenStep,
                                    actions.settings,
                                )
                            }
                        }
                    }
                }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    RefreshFeedbackCapsule(refreshState, onDismissFeedback, refreshProbe)
                }
            }
        }
    }
}

private fun LazyListScope.todayPanel(
    panel: TodayPanelUi,
    itemModifier: Modifier,
    clock: MinuteClock,
    language: Language,
    formatter: CopyLineFormatter,
    onOpenStep: (String) -> Unit,
    onSettings: () -> Unit,
) {
    if (panel.dayLabel != null) {
        item("panel.header", contentType = "panel.header") {
            TodayPanelHeader(panel, itemModifier.padding(top = PlanUITokens.space6.dp))
        }
    }
    item("section.now", contentType = "heading") {
        TodaySectionHeading(R.string.screens_right_now, language, itemModifier.padding(top = PlanUITokens.space6.dp))
    }
    if (panel.current.isEmpty()) {
        item("now.empty", contentType = "message") {
            NothingNow(itemModifier.padding(top = PlanUITokens.space2.dp))
        }
    }
    panel.current.forEachIndexed { index, row ->
        item("row.${row.blockId}", contentType = "row") {
            TodayStepRow(
                row,
                clock,
                language,
                formatter,
                onOpenStep,
                itemModifier.padding(
                    top =
                        (
                            if (index ==
                                0
                            ) {
                                PlanUITokens.space2
                            } else {
                                PlanUITokens.space1
                            }
                        ).dp,
                ),
            )
        }
    }
    item("section.next", contentType = "heading") {
        TodaySectionHeading(R.string.screens_up_next, language, itemModifier.padding(top = PlanUITokens.space6.dp))
    }
    if (panel.upcoming.isEmpty()) {
        item("next.empty", contentType = "message") {
            Text(
                stringResource(R.string.screens_no_more_scheduled_items),
                itemModifier.padding(top = PlanUITokens.space2.dp),
                color = CfTheme.colors.mutedForeground,
                style = CfTheme.type.style(CfTypeScale.screenBodyLg),
            )
        }
    }
    panel.upcoming.forEachIndexed { index, row ->
        item("row.${row.blockId}", contentType = "row") {
            TodayStepRow(
                row,
                clock,
                language,
                formatter,
                onOpenStep,
                itemModifier.padding(
                    top =
                        (
                            if (index ==
                                0
                            ) {
                                PlanUITokens.space2
                            } else {
                                PlanUITokens.space1
                            }
                        ).dp,
                ),
            )
        }
    }
    if (panel.hidden != HiddenNotice.None) {
        item("hidden", contentType = "hidden") {
            HiddenStepsLink(panel.hidden, onSettings, itemModifier.padding(top = PlanUITokens.space3.dp))
        }
    }
}
