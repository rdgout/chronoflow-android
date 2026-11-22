@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.feature.trips

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.icon.MaterialSymbols
import net.chronoflow.core.designsystem.layout.LocalSafeInsets
import net.chronoflow.core.designsystem.layout.WindowPolicy
import net.chronoflow.core.designsystem.system.CfMediumTopAppBar
import net.chronoflow.core.designsystem.system.CfPullToRefreshBox
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.model.config.ConfigurationRules
import net.chronoflow.core.model.config.LanguageChoice
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.sync.refresh.RefreshDebugProbe
import net.chronoflow.core.sync.refresh.RefreshFeedbackState
import net.chronoflow.core.ui.R
import net.chronoflow.core.ui.clock.MinuteClock
import net.chronoflow.core.ui.refresh.RefreshFeedbackCapsule
import net.chronoflow.core.ui.shell.LocalTabReselect
import net.chronoflow.core.ui.trip.TripCard
import net.chronoflow.core.ui.trip.tripsStatusText
import java.util.Locale

@Composable
fun TripsScreen(
    viewModel: TripsViewModel,
    onOpenTrip: (String) -> Unit,
    onNewTrip: () -> Unit,
) {
    val resolved = ConfigurationRules.resolveLanguage(LanguageChoice.System, LocalConfiguration.current.locales[0])
    val language = if (resolved == AppLanguage.Nl) Language.Nl else Language.En
    LaunchedEffect(viewModel, language) { viewModel.setLanguage(language) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshState by viewModel.refreshState.collectAsStateWithLifecycle()
    val now = viewModel.now.collectAsStateWithLifecycle()
    val clock = remember(now) { MinuteClock(now) }
    val formatter by viewModel.formatter.collectAsStateWithLifecycle()
    TripsContent(
        state,
        refreshState,
        language,
        viewModel::refresh,
        viewModel::dismissFeedback,
        onOpenTrip,
        onNewTrip,
        refreshProbe = viewModel.refreshProbe,
        clock = clock,
        formatter = formatter,
    )
}

@Composable
fun TripsContent(
    state: TripsUiState,
    refreshState: RefreshFeedbackState,
    language: Language,
    onRefresh: () -> Unit,
    onDismissFeedback: (Long) -> Unit,
    onOpenTrip: (String) -> Unit,
    onNewTrip: () -> Unit,
    modifier: Modifier = Modifier,
    refreshProbe: RefreshDebugProbe = RefreshDebugProbe.None,
    listState: LazyListState = rememberLazyListState(),
    clock: MinuteClock = remember { MinuteClock(mutableStateOf(java.time.Instant.EPOCH)) },
    formatter: CopyLineFormatter? = null,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val signal = LocalTabReselect.current
    var handled by rememberSaveable { mutableIntStateOf(signal) }
    val reduceMotion = CfTheme.reduceMotion
    LaunchedEffect(signal) {
        if (signal != handled) {
            handled = signal
            if (reduceMotion) listState.scrollToItem(0) else listState.animateScrollToItem(0)
            scrollBehavior.state.heightOffset = 0f
            scrollBehavior.state.contentOffset = 0f
        }
    }
    val insets = LocalSafeInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom).asPaddingValues()
    val padding =
        PaddingValues(
            start = PlanUITokens.space4.dp,
            end = PlanUITokens.space4.dp,
            top = PlanUITokens.space1.dp,
            bottom = PlanUITokens.space4.dp,
        ).plus(insets, LocalLayoutDirection.current)
    val itemModifier = Modifier.widthIn(max = WindowPolicy.listCardMaxWidthDp.dp).fillMaxWidth()
    Scaffold(
        modifier.fillMaxSize().testTag("trips.screen"),
        containerColor = CfTheme.colors.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            CfMediumTopAppBar(
                stringResource(
                    R.string.screens_my_trips,
                ),
                scrollBehavior,
                Modifier.testTag("trips.appbar"),
                actions = {
                    IconButton(onNewTrip, Modifier.testTag("trips.create")) {
                        Icon(MaterialSymbols.Add, stringResource(R.string.screens_new_trip))
                    }
                },
            )
        },
    ) { scaffoldPadding ->
        CfPullToRefreshBox(refreshState.refreshing, onRefresh, Modifier.fillMaxSize().padding(scaffoldPadding)) {
            // Consume downward overscroll in the app bar before pull-to-refresh.
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .testTag("trips.list"),
                state = listState,
                contentPadding = padding,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp),
            ) {
                when (state) {
                    TripsUiState.Loading -> {
                        item(key = "loading", contentType = "loading") {
                            TripsLoading(itemModifier)
                        }
                    }

                    TripsUiState.Empty -> {
                        item(key = "empty", contentType = "empty") {
                            TripsEmpty(onNewTrip, itemModifier)
                        }
                    }

                    is TripsUiState.Content -> {
                        state.sections.forEachIndexed { index, section ->
                            item(key = "section:${section.status.raw}", contentType = "header") {
                                val locale =
                                    if (language == Language.Nl) Locale.forLanguageTag("nl") else Locale.ENGLISH
                                val sectionPadding = if (index == 0) 0.dp else PlanUITokens.space3.dp
                                Text(
                                    tripsStatusText(section.status).uppercase(locale),
                                    itemModifier
                                        .padding(top = sectionPadding)
                                        .semantics { heading() }
                                        .testTag("trips.section.${section.status.raw}"),
                                    color = CfTheme.colors.mutedForeground,
                                    style = CfTheme.type.screenLabel,
                                )
                            }
                            items(section.trips, key = { it.id }, contentType = { "trip" }) { card ->
                                TripCard(
                                    card,
                                    { onOpenTrip(card.id) },
                                    clock,
                                    checkNotNull(formatter),
                                    itemModifier,
                                    language,
                                )
                            }
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

private fun PaddingValues.plus(
    other: PaddingValues,
    direction: LayoutDirection,
): PaddingValues =
    PaddingValues(
        start = calculateStartPadding(direction) + other.calculateStartPadding(direction),
        top = calculateTopPadding() + other.calculateTopPadding(),
        end = calculateEndPadding(direction) + other.calculateEndPadding(direction),
        bottom = calculateBottomPadding() + other.calculateBottomPadding(),
    )
