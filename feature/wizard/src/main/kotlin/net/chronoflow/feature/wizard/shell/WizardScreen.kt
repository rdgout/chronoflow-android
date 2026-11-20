@file:Suppress("ktlint:compose:modifier-missing-check")

package net.chronoflow.feature.wizard.shell

import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import net.chronoflow.core.designsystem.component.CfButton
import net.chronoflow.core.designsystem.component.CfButtonStyle
import net.chronoflow.core.designsystem.component.CfIconButton
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.haptics.HapticEvent
import net.chronoflow.core.designsystem.haptics.LocalHaptics
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.layout.LocalSafeInsets
import net.chronoflow.core.designsystem.system.CfLinearProgress
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.designsystem.theme.LocalReduceMotion
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.ui.R
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.feature.wizard.TripWizardViewModel
import net.chronoflow.feature.wizard.answers.WizardAnswerSheetHost
import net.chronoflow.feature.wizard.calculating.WizardCalculatingStep
import net.chronoflow.feature.wizard.checking.CheckingOutcome
import net.chronoflow.feature.wizard.checking.WizardCheckingStep
import net.chronoflow.feature.wizard.components.rememberServerCopy
import net.chronoflow.feature.wizard.excluded.WizardExcludedStep
import net.chronoflow.feature.wizard.finetune.WizardFineTuneStep
import net.chronoflow.feature.wizard.questions.WizardQuestionStep
import net.chronoflow.feature.wizard.ready.ReadyFooter
import net.chronoflow.feature.wizard.ready.ReadyFooterUi
import net.chronoflow.feature.wizard.ready.WizardReadyStep
import net.chronoflow.feature.wizard.returnchoice.WizardReturnStep
import net.chronoflow.feature.wizard.search.AirportSearchSheet
import net.chronoflow.feature.wizard.sleep.WizardSleepStep
import net.chronoflow.feature.wizard.sleep.WizardSleepTimeSheet
import net.chronoflow.feature.wizard.times.WizardTimesStep
import net.chronoflow.feature.wizard.ui.NavTitle
import net.chronoflow.feature.wizard.ui.WizardChromeUi
import net.chronoflow.feature.wizard.ui.WizardCopy
import net.chronoflow.feature.wizard.ui.WizardUiState
import net.chronoflow.feature.wizard.ui.resolve
import net.chronoflow.feature.wizard.where.WhereStep

@Composable
fun WizardRoot(
    viewModel: TripWizardViewModel,
    onExit: (WizardExit) -> Unit,
) {
    val language =
        net.chronoflow.core.model.contracts.Language
            .of(LocalConfiguration.current.locales[0].language)
    LaunchedEffect(viewModel, language) { viewModel.onEvent(WizardEvent.LanguageChanged(language)) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val formatter by viewModel.copyFormatter.collectAsStateWithLifecycle()
    val search by viewModel.searchState.collectAsStateWithLifecycle()
    val retryEnabled by viewModel.retryEnabled.collectAsStateWithLifecycle()
    val exit = rememberUpdatedState(onExit)
    val haptics = rememberUpdatedState(LocalHaptics.current)
    LaunchedEffect(viewModel) {
        viewModel.exit.collect { target ->
            if (target != null) {
                if (target is WizardExit.Saved &&
                    viewModel.consumeSavedHaptic()
                ) {
                    haptics.value.perform(HapticEvent.Success)
                }
                exit.value(target)
            }
        }
    }
    val events = remember(viewModel) { viewModel::onEvent }
    val active = state
    if (active == null) {
        Box(Modifier.fillMaxSize().background(CfTheme.colors.background))
    } else {
        WizardScreen(active.copy(search = search, retryEnabled = retryEnabled), formatter, events)
    }
}

@Composable
fun WizardScreen(
    state: WizardUiState,
    onEvent: (WizardEvent) -> Unit,
) = WizardScreen(state, null, onEvent)

@Composable
fun WizardScreen(
    state: WizardUiState,
    copyFormatter: CopyLineFormatter?,
    onEvent: (WizardEvent) -> Unit,
) {
    val haptics = LocalHaptics.current
    var handledCue by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(state.cue) {
        state.cue?.let { cue ->
            if (cue.sequence > handledCue) {
                handledCue = cue.sequence
                haptics.perform(
                    when (cue.outcome) {
                        CheckingOutcome.Success -> HapticEvent.Success
                        CheckingOutcome.Warning -> HapticEvent.Warning
                        CheckingOutcome.Error -> HapticEvent.Error
                    },
                )
            }
        }
    }
    WizardScaffold(state, onEvent, copyFormatter)
    state.sheet?.let { AirportSearchSheet(it, state.search, state.retryEnabled, onEvent) }
    state.sleep?.openTime?.let { WizardSleepTimeSheet(it, onEvent) }
    state.fineTune?.openTime?.let { WizardSleepTimeSheet(it, onEvent) }
    WizardAnswerSheetHost(state.answerSheet, onEvent, copyFormatter)
}

@Composable
internal fun WizardScaffold(
    state: WizardUiState,
    onEvent: (WizardEvent) -> Unit,
    copyFormatter: CopyLineFormatter? = null,
) {
    BoxWithConstraints(Modifier.fillMaxSize().background(CfTheme.colors.background).testTag("wizard.window")) {
        val safe = LocalSafeInsets.current
        val wide = constraints.maxWidth >= with(LocalDensity.current) { WizardMetrics.wideFrom.dp.roundToPx() }
        val column =
            if (wide) {
                Modifier.width(
                    WizardMetrics.column.dp,
                )
            } else {
                Modifier.fillMaxWidth().windowInsetsPadding(safe.only(WindowInsetsSides.Horizontal))
            }
        Box(Modifier.fillMaxSize().windowInsetsPadding(safe.only(WindowInsetsSides.Vertical)).imePadding()) {
            WizardHeader(state.chrome, onEvent)
            Box(
                column
                    .align(
                        Alignment.TopCenter,
                    ).padding(
                        top = (WizardMetrics.header + WizardMetrics.progressGap).dp,
                        start = WizardMetrics.gutter.dp,
                        end = WizardMetrics.gutter.dp,
                    ),
            ) {
                WizardProgress(state.chrome)
            }
            WizardStepHost(
                state,
                onEvent,
                column.align(Alignment.TopCenter).padding(top = WizardMetrics.contentStart.dp).fillMaxSize(),
                copyFormatter,
            )
        }
    }
}

@Composable
internal fun WizardHeader(
    chrome: WizardChromeUi,
    onEvent: (WizardEvent) -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                LocalSafeInsets.current.only(WindowInsetsSides.Horizontal),
            ).height(WizardMetrics.header.dp)
            .padding(horizontal = WizardMetrics.headerInset.dp)
            .testTag("wizard.header"),
    ) {
        if (chrome.showBack) {
            CfIconButton(
                LucideGlyphs.ChevronLeft.imageVector(
                    WizardMetrics.headerStroke,
                ),
                stringResource(R.string.auth_back),
                {
                    onEvent(WizardEvent.Back)
                },
                Modifier.align(Alignment.CenterStart).testTag("wizard.back"),
                iconSize = WizardMetrics.backGlyph.dp,
            )
        }
        Text(
            stringResource(
                if (chrome.navTitle ==
                    NavTitle.NewTrip
                ) {
                    R.string.wizard_new_trip
                } else {
                    R.string.wizard_edit_trip
                },
            ),
            Modifier
                .align(
                    Alignment.Center,
                ).padding(horizontal = WizardMetrics.target.dp)
                .testTag("wizard.nav-title"),
            color = CfTheme.colors.mutedForeground,
            style = CfTheme.type.style(CfTypeScale.wizardNavTitle),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
        if (chrome.showClose) {
            CfIconButton(
                LucideGlyphs.X.imageVector(
                    WizardMetrics.headerStroke,
                ),
                stringResource(R.string.wizard_close),
                {
                    onEvent(WizardEvent.RequestClose)
                },
                Modifier.align(Alignment.CenterEnd).testTag("wizard.close"),
                iconSize = WizardMetrics.closeGlyph.dp,
            )
        }
    }
}

@Composable
private fun WizardProgress(chrome: WizardChromeUi) {
    val shown by animateFloatAsState(
        chrome.progress,
        if (LocalReduceMotion.current) snap() else tween(WizardMetrics.progressMillis, easing = FastOutSlowInEasing),
        label = "wizard.progress",
    )
    val description = stringResource(R.string.wizard_progress_pct_percent, chrome.progressPercent)
    Box(
        Modifier
            .fillMaxWidth()
            .height(
                PlanUITokens.wizardProgressHeight.dp,
            ).testTag("wizard.progress")
            .clearAndSetSemantics {
                contentDescription = description
                progressBarRangeInfo = ProgressBarRangeInfo(chrome.progress, 0f..1f)
            },
    ) {
        CfLinearProgress({ shown })
    }
}

@Composable
internal fun WizardFooter(
    chrome: WizardChromeUi,
    step: WizardStepKey,
    onEvent: (WizardEvent) -> Unit,
) {
    val haptics = LocalHaptics.current
    val id = chrome.footerId
    if (id == null) {
        chrome.footerHint?.let {
            Text(
                it.resolve(),
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = WizardMetrics.gutter.dp,
                    ).padding(bottom = PlanUITokens.wizardHintBottom.dp)
                    .testTag(
                        if (step.step ==
                            MemberWizardStep.question
                        ) {
                            "wizard.question.hint"
                        } else {
                            "wizard.return.hint"
                        },
                    ),
                color = CfTheme.colors.mutedForeground,
                style = CfTheme.type.style(CfTypeScale.wizardHint),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
        return
    }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(
                horizontal = WizardMetrics.gutter.dp,
                vertical = WizardMetrics.footerInset.dp,
            ).testTag("wizard.$id.footer"),
        verticalArrangement = Arrangement.spacedBy(PlanUITokens.wizardControlGap.dp),
    ) {
        if (chrome.readyFooter == ReadyFooterUi.CloseOnly) {
            ReadyFooter(chrome.closeEnabled, onEvent)
            return@Column
        }
        if (chrome.secondaryClose) {
            CfButton(
                WizardCopy("Close").resolve(),
                { onEvent(if (chrome.secondaryRequestsClose) WizardEvent.CloseFromFooter else WizardEvent.CloseNow) },
                Modifier.testTag("wizard.$id.secondary-close"),
                style = CfButtonStyle.Ghost,
                fillWidth = true,
                minHeight = PlanUITokens.actionMinHeight.dp,
                textStyle = CfTheme.type.style(CfTypeScale.wizardSecondary),
                enabled = chrome.closeEnabled,
            )
        }
        CfButton(
            chrome.primaryLabel?.resolve() ?: if (chrome.primaryPlain) {
                WizardCopy(
                    "Change my answers",
                ).resolve()
            } else {
                stringResource(R.string.wizard_continue)
            },
            {
                if (step == WizardStepKey.Ready) haptics.perform(HapticEvent.Medium)
                onEvent(WizardEvent.Continue(step))
            },
            Modifier.testTag("wizard.$id.continue").semantics {
                if (chrome.primaryBusy) {
                    progressBarRangeInfo =
                        androidx.compose.ui.semantics.ProgressBarRangeInfo.Indeterminate
                }
            },
            fillWidth = true,
            minHeight = PlanUITokens.wizardButtonHeight.dp,
            textStyle = CfTheme.type.style(CfTypeScale.wizardButton),
            enabled = chrome.continueEnabled,
            loading = chrome.primaryBusy,
            loadingDescription = chrome.primaryLabel?.resolve().orEmpty(),
            disabledAlpha =
                if (chrome.primaryBusy) {
                    WizardMetrics.ctaBusyAlpha
                } else if (chrome.routeComplete) {
                    1f
                } else {
                    PlanUITokens.wizardDisabledOpacity
                },
            containerColor = if (chrome.primaryPlain) CfTheme.colors.surface2 else CfTheme.colors.primary,
            contentColor = if (chrome.primaryPlain) CfTheme.colors.foreground else CfTheme.colors.primaryForeground,
        )
    }
}

@Composable
internal fun WizardStepHost(
    state: WizardUiState,
    onEvent: (WizardEvent) -> Unit,
    modifier: Modifier = Modifier,
    copyFormatter: CopyLineFormatter? = null,
) {
    val reduce = LocalReduceMotion.current
    val events = rememberUpdatedState(onEvent)
    val stack = listOf(WizardStepKey.Where) + state.path
    val rendered = remember { mutableMapOf<WizardStepKey, WizardUiState>() }
    SideEffect { rendered[state.step] = state }
    NavDisplay(
        entries =
            stack.map { key ->
                NavEntry(key) {
                    WizardStepContent(
                        if (key ==
                            state.step
                        ) {
                            state
                        } else {
                            rendered[key] ?: state
                        },
                        key,
                        events.value,
                        copyFormatter,
                    )
                }
            },
        onBack = { if (state.step != WizardStepKey.Calculating) onEvent(WizardEvent.Back) },
        modifier = modifier,
        transitionSpec = {
            if (reduce) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                slideInHorizontally(tween(WizardMetrics.transitionMillis)) {
                    it
                } togetherWith slideOutHorizontally(tween(WizardMetrics.transitionMillis)) { -it / 4 }
            }
        },
        popTransitionSpec = {
            if (reduce) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                slideInHorizontally(tween(WizardMetrics.transitionMillis)) {
                    -it / 4
                } togetherWith slideOutHorizontally(tween(WizardMetrics.transitionMillis)) { it }
            }
        },
        predictivePopTransitionSpec = {
            if (reduce) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                slideInHorizontally(tween(WizardMetrics.transitionMillis)) {
                    -it / 4
                } togetherWith slideOutHorizontally(tween(WizardMetrics.transitionMillis)) { it }
            }
        },
    )
    // BoxWithConstraints subcomposes this host; these handlers must register after NavDisplay.
    WizardBackHandling(state, onEvent)
    BackHandler(enabled = state.step == WizardStepKey.Calculating) {}
}

@Composable
private fun WizardStepContent(
    state: WizardUiState,
    key: WizardStepKey,
    onEvent: (WizardEvent) -> Unit,
    copyFormatter: CopyLineFormatter?,
) {
    val where = state.where
    val times = state.times
    val titleCopy =
        where?.title ?: times?.title ?: state.returnChoice?.title ?: state.sleep?.title ?: state.excluded?.title
            ?: state.checking?.title ?: state.fineTune?.title ?: state.ready?.title ?: state.calculating?.title
    val subtitleCopy =
        where?.subtitle ?: times?.subtitle ?: state.sleep?.subtitle ?: state.checking?.subtitle
            ?: state.fineTune?.subtitle ?: state.calculating?.subtitle
    val serverCopy = rememberServerCopy(copyFormatter)
    val title = titleCopy?.resolve() ?: state.question?.let { serverCopy(it.title) }
    val heading: @Composable () -> Unit = {
        if (title != null) WizardStepHeading(state, key, title, subtitleCopy)
    }
    Column(Modifier.fillMaxSize().wizardBuiltStepBoundary(key)) {
        val scroll =
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("wizard.scroll")
                .verticalScroll(rememberScrollState())
                .padding(horizontal = WizardMetrics.gutter.dp, vertical = WizardMetrics.contentTop.dp)
        val fineTune = state.fineTune
        if (fineTune != null) {
            WizardFineTuneStep(fineTune, copyFormatter, onEvent, scroll, heading)
        } else {
            Column(scroll, verticalArrangement = Arrangement.spacedBy(PlanUITokens.wizardContentGap.dp)) {
                if (title != null) {
                    heading()
                    when {
                        state.calculating != null -> WizardCalculatingStep(state.calculating)
                        state.ready != null -> WizardReadyStep(state.ready, copyFormatter, onEvent)
                        state.checking != null -> WizardCheckingStep(state.checking)
                        state.sleep != null -> WizardSleepStep(state.sleep, onEvent, copyFormatter)
                        state.question != null -> WizardQuestionStep(state.question, onEvent)
                        state.excluded != null -> WizardExcludedStep(state.excluded, copyFormatter)
                        where != null -> WhereStep(where, copyFormatter, onEvent)
                        times != null -> WizardTimesStep(times, onEvent, copyFormatter)
                        state.returnChoice != null -> WizardReturnStep(state.returnChoice, onEvent)
                    }
                } else if (key.step != MemberWizardStep.question) {
                    WizardStepSeam(key)
                }
            }
        }
        if (key == state.step) WizardFooter(state.chrome, key, onEvent)
    }
}

@Composable
private fun WizardStepHeading(
    state: WizardUiState,
    key: WizardStepKey,
    title: String,
    subtitleCopy: WizardCopy?,
) {
    val titleFocus = remember { FocusRequester() }
    val locale = LocalConfiguration.current.locales[0]
    Column(
        verticalArrangement = Arrangement.spacedBy(WizardMetrics.titleGap.dp),
        modifier = Modifier.padding(bottom = WizardMetrics.titleExtra.dp),
    ) {
        state.question?.eyebrow?.let {
            Text(
                it.resolve().uppercase(locale),
                Modifier.testTag("wizard.eyebrow"),
                color = CfTheme.colors.dawnText,
                style = CfTheme.type.style(CfTypeScale.screenEyebrow),
            )
        }
        Text(
            title,
            Modifier
                .testTag("wizard.title")
                .focusRequester(titleFocus)
                .focusable()
                .semantics {
                    heading()
                    if (key == WizardStepKey.Calculating) liveRegion = LiveRegionMode.Polite
                },
            color = CfTheme.colors.foreground,
            style =
                CfTheme.type.style(
                    if (title.length >
                        80
                    ) {
                        CfTypeScale.wizardLongTitle
                    } else {
                        CfTypeScale.screenPageTitle
                    },
                ),
        )
        if (subtitleCopy != null) {
            Text(
                subtitleCopy.resolve(),
                Modifier.testTag("wizard.subtitle"),
                color = CfTheme.colors.mutedForeground,
                style = CfTheme.type.style(CfTypeScale.wizardSubtitle),
            )
        }
    }
    LaunchedEffect(key) { titleFocus.requestFocus() }
}
