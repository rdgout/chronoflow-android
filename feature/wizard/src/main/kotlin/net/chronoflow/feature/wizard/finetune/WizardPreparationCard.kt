package net.chronoflow.feature.wizard.finetune

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfDivider
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.haptics.HapticEvent
import net.chronoflow.core.designsystem.haptics.LocalHaptics
import net.chronoflow.core.designsystem.system.CfSwitch
import net.chronoflow.core.designsystem.system.cfToggleable
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.wizard.model.WizardForm
import net.chronoflow.feature.wizard.components.WizardErrorLine
import net.chronoflow.feature.wizard.components.WizardStepper
import net.chronoflow.feature.wizard.components.WizardTimeRow
import net.chronoflow.feature.wizard.components.fieldCopy
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardMetrics
import net.chronoflow.feature.wizard.sleep.WizardSleepCard
import net.chronoflow.feature.wizard.ui.WizardCopy
import net.chronoflow.feature.wizard.ui.resolve

@Composable
internal fun WizardPreparationCard(
    ui: WizardFineTuneUi,
    formatter: CopyLineFormatter?,
    onEvent: (WizardEvent) -> Unit,
) {
    val c = CfTheme.colors
    val haptics = LocalHaptics.current
    WizardSleepCard {
        Column(Modifier.testTag("wizard.preparation-card")) {
            Row(
                Modifier
                    .heightIn(min = PlanUITokens.wizardButtonHeight.dp)
                    .padding(start = PlanUITokens.space4.dp, end = PlanUITokens.space3.dp),
                horizontalArrangement = Arrangement.spacedBy(WizardMetrics.preparationControlGap.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        WizardCopy("Preparation days").resolve(),
                        color = c.foreground,
                        style = CfTheme.type.style(CfTypeScale.wizardSearchName),
                    )
                    Text(
                        WizardCopy("Before you leave").resolve(),
                        color = c.mutedForeground,
                        style = CfTheme.type.style(CfTypeScale.wizardHint),
                    )
                }
                Text(
                    ui.preparationDays.toString(),
                    Modifier.widthIn(min = PlanUITokens.wizardContentGap.dp),
                    color = c.foreground,
                    style = CfTheme.type.style(CfTypeScale.wizardButton).copy(fontFeatureSettings = "tnum"),
                    textAlign = TextAlign.Center,
                )
                WizardStepper(
                    ui.preparationDays,
                    0..WizardForm.maxPreparationDays,
                    WizardCopy(
                        "Preparation days",
                    ).resolve(),
                    {
                        haptics.perform(HapticEvent.Selection)
                        onEvent(WizardEvent.SetPreparationDays(it))
                    },
                    testTag = "wizard.preparation",
                    enabled = !ui.reviewing,
                )
            }
            if (ui.showSleepSwitch) {
                CfDivider()
                Row(
                    Modifier
                        .testTag("wizard.sleep-override")
                        .cfToggleable(ui.sleepOverride, {
                            onEvent(WizardEvent.SetSleepOverride(it))
                        }, enabled = !ui.reviewing)
                        .heightIn(min = PlanUITokens.wizardButtonHeight.dp)
                        .padding(horizontal = PlanUITokens.space4.dp, vertical = PlanUITokens.wizardControlGap.dp),
                    horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        WizardCopy("Use different sleep times for this trip").resolve(),
                        Modifier.weight(1f),
                        color = c.foreground,
                        style = CfTheme.type.style(CfTypeScale.wizardSearchName),
                    )
                    CfSwitch(ui.sleepOverride, null)
                }
            }
            if (ui.showSleepSwitch && ui.sleepOverride) {
                CfDivider()
                WizardTimeRow(
                    WizardCopy("Bedtime on this trip"),
                    ui.overrideBed,
                    ui.timeFormat,
                    { onEvent(WizardEvent.OpenTimeSheet("override.habitual_bed")) },
                    "wizard.override.bed",
                    minHeight = PlanUITokens.actionMinHeight.dp,
                    enabled = !ui.reviewing,
                    labelStyle = CfTheme.type.style(CfTypeScale.wizardSearchName),
                )
                CfDivider()
                WizardTimeRow(
                    WizardCopy("Wake-up on this trip"),
                    ui.overrideWake,
                    ui.timeFormat,
                    { onEvent(WizardEvent.OpenTimeSheet("override.habitual_wake")) },
                    "wizard.override.wake",
                    minHeight = PlanUITokens.actionMinHeight.dp,
                    enabled = !ui.reviewing,
                    labelStyle = CfTheme.type.style(CfTypeScale.wizardSearchName),
                )
                Text(
                    WizardCopy("Only for this trip. Your usual sleep stays saved.").resolve(),
                    Modifier.padding(
                        start = PlanUITokens.space4.dp,
                        end = PlanUITokens.space4.dp,
                        bottom = PlanUITokens.space3.dp,
                    ),
                    color = c.mutedForeground,
                    style = CfTheme.type.style(CfTypeScale.wizardHint),
                )
            }
            ui.fineTuneErrors.forEach { (key, message) ->
                Column(Modifier.padding(PlanUITokens.space4.dp)) {
                    WizardErrorLine(fieldCopy(message, formatter), "wizard.fine-tune.error.$key")
                }
            }
        }
    }
}
