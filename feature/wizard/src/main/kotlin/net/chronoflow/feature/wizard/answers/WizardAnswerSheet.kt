@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.feature.wizard.answers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.designsystem.component.CfButton
import net.chronoflow.core.designsystem.component.CfButtonStyle
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.haptics.HapticEvent
import net.chronoflow.core.designsystem.haptics.LocalHaptics
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.system.CfModalBottomSheet
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.model.contracts.PromptKind
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.feature.wizard.components.WizardErrorLine
import net.chronoflow.feature.wizard.components.WizardTimeRow
import net.chronoflow.feature.wizard.components.fieldCopy
import net.chronoflow.feature.wizard.components.rememberServerCopy
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardMetrics
import net.chronoflow.feature.wizard.sleep.WizardSleepTimeSheet
import net.chronoflow.feature.wizard.sleep.clockString
import net.chronoflow.feature.wizard.ui.WizardCopy
import net.chronoflow.feature.wizard.ui.resolve

@Composable
internal fun WizardAnswerSheetHost(
    ui: WizardAnswerSheetUi?,
    onEvent: (WizardEvent) -> Unit,
    formatter: CopyLineFormatter? = null,
) {
    if (ui != null && ui.state.prompt.kind in SUPPORTED_SHEET_KINDS) {
        WizardAnswerSheet(ui, onEvent, formatter)
    }
}

@Composable
internal fun WizardAnswerSheet(
    ui: WizardAnswerSheetUi,
    onEvent: (WizardEvent) -> Unit,
    formatter: CopyLineFormatter? = null,
) {
    val haptics = LocalHaptics.current
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focus = remember { FocusRequester() }
    val serverCopy = rememberServerCopy(formatter)
    var handled by rememberSaveable(ui.state.prompt.id) { mutableIntStateOf(ui.state.failures) }
    LaunchedEffect(ui.state.failures) {
        if (ui.state.failures > handled) haptics.perform(HapticEvent.Warning)
        handled = ui.state.failures
    }
    LaunchedEffect(sheet) {
        snapshotFlow { sheet.currentValue }.first { it == SheetValue.Expanded }
        focus.requestFocus()
    }
    CfModalBottomSheet({ onEvent(WizardEvent.CancelAnswer) }, sheetState = sheet) {
        Column(
            Modifier
                .fillMaxWidth()
                .testTag(
                    if (ui.state.prompt.kind == PromptKind.LatestWake) {
                        "wizard.answer.latest_wake.sheet"
                    } else {
                        "wizard.answer.${ui.state.prompt.kind.raw}"
                    },
                ).navigationBarsPadding()
                .imePadding()
                .padding(
                    top = PlanUITokens.wizardChronotypePadding.dp,
                    start = PlanUITokens.space5.dp,
                    end = PlanUITokens.space5.dp,
                    bottom = (PlanUITokens.space8 + PlanUITokens.wizardLabelGap).dp,
                ),
        ) {
            WizardAnswerHeader(ui, focus, onEvent)
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .testTag("wizard.answer.scroll")
                    .padding(top = PlanUITokens.wizardContentGap.dp),
                verticalArrangement = Arrangement.spacedBy(PlanUITokens.wizardContentGap.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp)) {
                    Text(
                        serverCopy(ui.state.prompt.text),
                        Modifier.testTag("wizard.answer.question"),
                        color = CfTheme.colors.foreground,
                        style = CfTheme.type.style(CfTypeScale.wizardRow).copy(fontWeight = FontWeight.SemiBold),
                    )
                    if (ui.state.prompt.kind == PromptKind.SeatSide) {
                        ui.route?.let {
                            Text(
                                it,
                                color = CfTheme.colors.mutedForeground,
                                style = CfTheme.type.style(CfTypeScale.wizardHint),
                            )
                        }
                    }
                }
                if (ui.state.prompt.kind == PromptKind.LightDevice) {
                    Text(
                        WizardCopy(
                            "Don't use a light box or light glasses if you have an eye disease, have had recent eye surgery or take medicine that makes your eyes sensitive to light. Ask your doctor first.",
                        ).resolve(),
                        Modifier.testTag("wizard.answer.lightNotice"),
                        color = CfTheme.colors.mutedForeground,
                        style = CfTheme.type.style(CfTypeScale.wizardHint),
                    )
                }
                WizardAnswerOptions(ui.state, onEvent)
                when (ui.state.prompt.kind) {
                    PromptKind.FreeDayTimes -> {
                        WizardFreeDayControl(ui, onEvent)
                    }

                    PromptKind.Room -> {
                        WizardRoomControl(ui, onEvent)
                    }

                    PromptKind.LatestWake -> {
                        WizardWakeControl(ui, onEvent)
                    }

                    PromptKind.PreDepartureBusyHours -> {
                        WizardBusyControl(ui, onEvent)
                    }

                    PromptKind.Cups -> {
                        WizardDrinksControl(ui.state, onEvent)
                    }

                    PromptKind.BlueBlockers -> {
                        WizardRatingControl(ui.state, onEvent)
                    }

                    else -> {}
                }
                ui.state.error?.let { WizardErrorLine(fieldCopy(it, formatter), "wizard.answer.error") }
            }
        }
    }
    ui.openTime?.let { WizardSleepTimeSheet(it, onEvent) }
}

@Composable
private fun WizardAnswerHeader(
    ui: WizardAnswerSheetUi,
    focus: FocusRequester,
    onEvent: (WizardEvent) -> Unit,
) {
    Layout(
        content = {
            CfButton(
                WizardCopy("Cancel").resolve(),
                { onEvent(WizardEvent.CancelAnswer) },
                Modifier.testTag("wizard.answer.cancel"),
                style = CfButtonStyle.Ghost,
                minHeight = PlanUITokens.actionMinHeight.dp,
                textStyle = CfTheme.type.style(CfTypeScale.wizardRow),
                contentColor = CfTheme.colors.dawnText,
                contentPadding = PaddingValues(0.dp),
            )
            Text(
                WizardCopy(ui.labelKey).resolve() + ui.suffix.orEmpty(),
                Modifier
                    .testTag("wizard.answer.title")
                    .focusRequester(focus)
                    .focusable()
                    .semantics { heading() },
                color = CfTheme.colors.foreground,
                style = CfTheme.type.style(CfTypeScale.wizardSheetTitle),
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            CfButton(
                WizardCopy("Done").resolve(),
                { onEvent(WizardEvent.AnswerDone) },
                Modifier.testTag("wizard.answer.done"),
                style = CfButtonStyle.Ghost,
                minHeight = PlanUITokens.actionMinHeight.dp,
                enabled = ui.state.doneEnabled,
                disabledAlpha = PlanUITokens.wizardEmptyOpacity,
                textStyle = CfTheme.type.style(CfTypeScale.wizardButton),
                contentColor = CfTheme.colors.dawnText,
                contentPadding = PaddingValues(0.dp),
            )
        },
        modifier = Modifier.fillMaxWidth().heightIn(min = PlanUITokens.actionMinHeight.dp),
    ) { measurables, constraints ->
        val target = PlanUITokens.actionMinHeight.dp.roundToPx()
        val gap = PlanUITokens.space2.dp.roundToPx()
        val sideLimit = ((constraints.maxWidth - gap) / 2).coerceAtLeast(target)
        val leadingWidth =
            maxOf(
                target,
                measurables[0].maxIntrinsicWidth(constraints.maxHeight),
            ).coerceAtMost(sideLimit)
        val trailingWidth =
            maxOf(
                target,
                measurables[2].maxIntrinsicWidth(constraints.maxHeight),
            ).coerceAtMost(sideLimit)
        val inlineWidth = (constraints.maxWidth - 2 * maxOf(leadingWidth, trailingWidth) - 2 * gap).coerceAtLeast(0)
        val naturalTitle = measurables[1].maxIntrinsicWidth(constraints.maxHeight)
        val wrap = inlineWidth < maxOf(measurables[1].minIntrinsicWidth(constraints.maxHeight), (naturalTitle + 1) / 2)
        val titleWidth =
            minOf(
                WizardMetrics.sheetTitleMaxWidth.dp.roundToPx(),
                naturalTitle,
                if (wrap) constraints.maxWidth else inlineWidth,
            )
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val title = measurables[1].measure(loose.copy(maxWidth = titleWidth))
        val leading = measurables[0].measure(loose.copy(minWidth = leadingWidth, maxWidth = leadingWidth))
        val trailing = measurables[2].measure(loose.copy(minWidth = trailingWidth, maxWidth = trailingWidth))
        val actionsHeight = maxOf(leading.height, trailing.height)
        val height = if (wrap) title.height + gap + actionsHeight else maxOf(title.height, actionsHeight)
        layout(constraints.maxWidth, constraints.constrainHeight(height)) {
            val actionY = if (wrap) title.height + gap else (height - actionsHeight) / 2
            leading.placeRelative(0, actionY + (actionsHeight - leading.height) / 2)
            trailing.placeRelative(
                constraints.maxWidth - trailing.width,
                actionY + (actionsHeight - trailing.height) / 2,
            )
            title.placeRelative((constraints.maxWidth - title.width) / 2, if (wrap) 0 else (height - title.height) / 2)
        }
    }
}

@Composable
internal fun WizardAnswerCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(PlanUITokens.wizardCardRadius.dp)
    Box(
        modifier
            .fillMaxWidth()
            .background(CfTheme.colors.background, shape)
            .border(AuthTokens.borderWidth.dp, CfTheme.colors.border, shape),
    ) {
        Column { content() }
    }
}
