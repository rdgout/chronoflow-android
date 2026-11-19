package net.chronoflow.core.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfBadge
import net.chronoflow.core.designsystem.component.CfBadgeVariant
import net.chronoflow.core.designsystem.component.CfButton
import net.chronoflow.core.designsystem.component.CfButtonSize
import net.chronoflow.core.designsystem.component.CfButtonStyle
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.designsystem.theme.tnum
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.plan.step.StepActions
import net.chronoflow.core.plan.step.StepBadge
import net.chronoflow.core.plan.step.StepSheetState
import net.chronoflow.core.ui.R

@Composable
fun StepSheetContent(
    state: StepSheetState,
    busy: Boolean,
    onCheck: (CheckStatus?) -> Unit,
    onSeeToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val announcer = LocalPoliteAnnouncer.current ?: remember { PoliteAnnouncer() }
    CompositionLocalProvider(LocalPoliteAnnouncer provides announcer) {
        Column(modifier.testTag("step.sheet")) {
            if (state is StepSheetState.Step) {
                StepContent(state, busy, onCheck, Modifier.weight(1f, fill = false))
            } else {
                StepGoneContent(onSeeToday, Modifier.weight(1f, fill = false))
            }
            RefusedChangeFeedback()
            PoliteAnnouncerHost(announcer)
        }
    }
}

@Composable
private fun StepContent(
    state: StepSheetState.Step,
    busy: Boolean,
    onCheck: (CheckStatus?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = CfTheme.colors
    val undoFocus = remember { FocusRequester() }
    val doneFocus = remember { FocusRequester() }
    val announcer = LocalPoliteAnnouncer.current
    val mark = state.badge?.takeIf { it != StepBadge.Now }
    var previous by remember { mutableStateOf(state.blockId to mark) }
    var pendingFocus by remember { mutableStateOf<StepActions?>(null) }
    val announcement =
        stringResource(
            when (mark) {
                StepBadge.Done -> R.string.screens_done
                StepBadge.Skipped -> R.string.screens_skipped
                else -> R.string.screens_check_off_undone
            },
        )
    LaunchedEffect(state.blockId, mark, busy) {
        if (previous.first == state.blockId && previous.second != mark) {
            announcer?.announce(announcement)
            pendingFocus = state.actions
        } else if (previous.first != state.blockId) {
            pendingFocus = null
        }
        if (!busy) {
            if (pendingFocus == StepActions.Marked) undoFocus.requestFocus()
            if (pendingFocus == StepActions.Open) doneFocus.requestFocus()
            pendingFocus = null
        }
        previous = state.blockId to mark
    }
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(
                start = PlanUITokens.space5.dp,
                end = PlanUITokens.space5.dp,
                top = PlanUITokens.space3.dp,
                bottom = PlanUITokens.space8.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(PlanUITokens.space4.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp), verticalAlignment = Alignment.Top) {
            CategorySwatch(state.type)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PlanUITokens.space1.dp)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp),
                    verticalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp),
                ) {
                    PriorityTag(planScreenText(state.priorityKey))
                    state.badge?.let { badge ->
                        CfBadge(
                            stringResource(
                                when (badge) {
                                    StepBadge.Done -> R.string.screens_done
                                    StepBadge.Skipped -> R.string.screens_skipped
                                    StepBadge.Now -> R.string.screens_now
                                },
                            ),
                            variant =
                                when (badge) {
                                    StepBadge.Done -> CfBadgeVariant.Secondary
                                    StepBadge.Skipped -> CfBadgeVariant.Outline
                                    StepBadge.Now -> CfBadgeVariant.Dawn
                                },
                        )
                    }
                }
                val titleStyle = CfTheme.type.style(CfTypeScale.screenCardTitle)
                val minTitle = with(LocalDensity.current) { titleStyle.lineHeight.toDp() }
                Text(
                    planScreenText(state.titleKey),
                    Modifier
                        .heightIn(min = minTitle)
                        .semantics { heading() }
                        .testTag("step.title"),
                    color = c.foreground,
                    style = titleStyle,
                )
                val time = stringResource(R.string.screens_time_zone_time, state.interval, state.zone)
                Text(
                    time +
                        (state.durationLeft?.let { " · " + stringResource(R.string.screens_duration_left, it) } ?: ""),
                    Modifier.testTag("step.time"),
                    color = c.mutedForeground,
                    style = CfTheme.type.screenBody.tnum(),
                )
            }
        }
        if (state.reasons.isNotEmpty()) StepSection(R.string.screens_why, state.reasons, false)
        StepSection(R.string.screens_how, state.instructions, true)
        when (state.actions) {
            StepActions.Marked -> {
                CfButton(
                    stringResource(R.string.screens_undo),
                    { onCheck(null) },
                    Modifier.testTag("step.undo").focusRequester(undoFocus).focusProperties {
                        canFocus = true
                    },
                    style = CfButtonStyle.Outline,
                    size = CfButtonSize.Lg,
                    enabled = !busy,
                    fillWidth = true,
                    minHeight = PlanUITokens.actionMinHeight.dp,
                )
            }

            StepActions.Open -> {
                StepButtons(busy, doneFocus, onCheck)
            }

            StepActions.None -> {}
        }
        Text(
            stringResource(R.string.screens_checking_off_changes_your_progres_37ae7d),
            Modifier.fillMaxWidth(),
            color = c.mutedForeground,
            style = CfTheme.type.style(CfTypeScale.screenMeta),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StepSection(
    title: Int,
    lines: List<String>,
    bullets: Boolean,
) {
    val locale = LocalConfiguration.current.locales[0]
    Column(verticalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp)) {
        Text(
            stringResource(title).uppercase(locale),
            Modifier.semantics { heading() }.testTag("step.section.$title"),
            color = CfTheme.colors.mutedForeground,
            style = CfTheme.type.screenLabel,
        )
        lines.forEach { line ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp),
                verticalAlignment = Alignment.Top,
            ) {
                if (bullets) {
                    Box(
                        Modifier
                            .padding(top = PlanUITokens.space2.dp)
                            .size(PlanUITokens.space1.dp)
                            .background(CfTheme.colors.foreground, CircleShape),
                    )
                }
                Text(line, color = CfTheme.colors.foreground, style = CfTheme.type.style(CfTypeScale.screenBodyLg))
            }
        }
    }
}

@Composable
private fun StepButtons(
    busy: Boolean,
    doneFocus: FocusRequester,
    onCheck: (CheckStatus?) -> Unit,
) {
    val skip = stringResource(R.string.screens_skip)
    val done = stringResource(R.string.screens_mark_as_done)
    val labelStyle = CfTheme.type.authLabel
    SubcomposeLayout { constraints ->
        val gap = PlanUITokens.space2.dp.roundToPx()
        val padding = (PlanUITokens.space6 * 2).dp.roundToPx()
        val icon = (PlanMetrics.icon + AuthTokens.fieldGap).dp.roundToPx()
        val skipWidth =
            subcompose(
                "skip-label",
            ) { Text(skip, style = labelStyle) }.single().measure(Constraints()).width
        val doneWidth =
            subcompose(
                "done-label",
            ) { Text(done, style = labelStyle) }.single().measure(Constraints()).width
        val stacked = maxOf(skipWidth + padding, doneWidth + padding + icon) * 2 + gap > constraints.maxWidth
        val buttons =
            subcompose("buttons") {
                val skipButton: @Composable (Modifier) -> Unit = { m ->
                    CfButton(
                        skip,
                        { onCheck(CheckStatus.Skipped) },
                        m.testTag("step.skip"),
                        style = CfButtonStyle.Outline,
                        size = CfButtonSize.Lg,
                        enabled = !busy,
                        fillWidth = true,
                        minHeight = PlanUITokens.actionMinHeight.dp,
                    )
                }
                val doneButton: @Composable (Modifier) -> Unit = { m ->
                    CfButton(
                        done,
                        { onCheck(CheckStatus.Done) },
                        m.testTag("step.done").focusRequester(doneFocus).focusProperties {
                            canFocus =
                                true
                        },
                        leadingIcon = LucideGlyphs.Check.imageVector(2.5f),
                        leadingIconSize = PlanMetrics.icon.dp,
                        size = CfButtonSize.Lg,
                        enabled = !busy,
                        fillWidth = true,
                        minHeight = PlanUITokens.actionMinHeight.dp,
                    )
                }
                if (stacked) {
                    Column(verticalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp)) {
                        skipButton(Modifier)
                        doneButton(Modifier)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp)) {
                        skipButton(Modifier.weight(1f))
                        doneButton(Modifier.weight(1f))
                    }
                }
            }.single().measure(constraints.copy(minWidth = constraints.maxWidth, minHeight = 0))
        layout(buttons.width, buttons.height) { buttons.placeRelative(0, 0) }
    }
}
