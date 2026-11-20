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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
                .testTag("wizard.answer.${ui.state.prompt.kind.raw}")
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(
                    top = PlanUITokens.wizardChronotypePadding.dp,
                    start = PlanUITokens.space5.dp,
                    end = PlanUITokens.space5.dp,
                    bottom = (PlanUITokens.space8 + PlanUITokens.wizardLabelGap).dp,
                ),
            verticalArrangement = Arrangement.spacedBy(PlanUITokens.wizardContentGap.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = PlanUITokens.actionMinHeight.dp),
                horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
                    WizardCopy(WizardPromptLabels.key(ui.state.prompt)).resolve(),
                    Modifier
                        .weight(1f)
                        .widthIn(max = WizardMetrics.sheetTitleMaxWidth.dp)
                        .testTag("wizard.answer.title")
                        .focusRequester(focus)
                        .focusable()
                        .semantics { heading() },
                    color = CfTheme.colors.foreground,
                    style = CfTheme.type.style(CfTypeScale.wizardButton),
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
            }
            Text(
                serverCopy(ui.state.prompt.text),
                Modifier.testTag("wizard.answer.question"),
                color = CfTheme.colors.foreground,
                style = CfTheme.type.style(CfTypeScale.wizardRow).copy(fontWeight = FontWeight.SemiBold),
            )
            WizardAnswerCard {
                Column(Modifier.selectableGroup().testTag("wizard.answer.options")) {
                    WizardFreeDayOption(ui, 0, "same_as_workdays", "Same as work days", onEvent)
                    HorizontalDivider(color = CfTheme.colors.border)
                    WizardFreeDayOption(ui, 1, "declared", "Different times", onEvent)
                }
            }
            if (ui.declared) {
                WizardAnswerCard {
                    WizardTimeRow(
                        WizardCopy("Bedtime"),
                        ui.state.values["free_day_bed"].clockString(),
                        ui.timeFormat,
                        { onEvent(WizardEvent.OpenTimeSheet("free_day_bed")) },
                        "wizard.answer.time.free_day_bed",
                        minHeight = WizardMetrics.sheetRowMinHeight.dp,
                    )
                    HorizontalDivider(color = CfTheme.colors.border)
                    WizardTimeRow(
                        WizardCopy("Wake-up"),
                        ui.state.values["free_day_wake"].clockString(),
                        ui.timeFormat,
                        { onEvent(WizardEvent.OpenTimeSheet("free_day_wake")) },
                        "wizard.answer.time.free_day_wake",
                        minHeight = WizardMetrics.sheetRowMinHeight.dp,
                    )
                }
            }
            ui.state.error?.let { WizardErrorLine(fieldCopy(it, formatter), "wizard.answer.error") }
        }
    }
    ui.openTime?.let { WizardSleepTimeSheet(it, onEvent) }
}

@Composable
private fun WizardFreeDayOption(
    ui: WizardAnswerSheetUi,
    index: Int,
    value: String,
    label: String,
    onEvent: (WizardEvent) -> Unit,
) {
    val selected = ui.state.values["free_day_status"] == JsonPrimitive(value)
    val haptics = LocalHaptics.current
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = WizardMetrics.sheetRowMinHeight.dp)
            .testTag("wizard.answer.option.$index")
            .selectable(selected, role = Role.RadioButton) {
                haptics.perform(HapticEvent.Selection)
                onEvent(WizardEvent.UpdateAnswer("free_day_status", JsonPrimitive(value)))
            }.padding(horizontal = PlanUITokens.space4.dp, vertical = PlanUITokens.space2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp),
    ) {
        Text(
            WizardCopy(label).resolve(),
            Modifier.weight(1f),
            color = CfTheme.colors.foreground,
            style = CfTheme.type.style(CfTypeScale.wizardRow),
        )
        if (selected) {
            Icon(
                LucideGlyphs.Check.imageVector(),
                null,
                Modifier.size(PlanUITokens.space5.dp),
                tint = CfTheme.colors.dawnText,
            )
        }
    }
}

@Composable
private fun WizardAnswerCard(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(PlanUITokens.wizardCardRadius.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .background(CfTheme.colors.background, shape)
            .border(AuthTokens.borderWidth.dp, CfTheme.colors.border, shape),
    ) {
        Column { content() }
    }
}
