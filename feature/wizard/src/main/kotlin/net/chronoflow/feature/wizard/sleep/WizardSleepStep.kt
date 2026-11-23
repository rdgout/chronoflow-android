package net.chronoflow.feature.wizard.sleep

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.haptics.HapticEvent
import net.chronoflow.core.designsystem.haptics.LocalHaptics
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.feature.wizard.components.WizardChoiceCard
import net.chronoflow.feature.wizard.components.WizardChoiceStyle
import net.chronoflow.feature.wizard.components.WizardErrorLine
import net.chronoflow.feature.wizard.components.WizardTimeRow
import net.chronoflow.feature.wizard.components.fieldCopy
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.ui.WizardCopy
import net.chronoflow.feature.wizard.ui.resolve

@Composable
internal fun WizardSleepStep(
    ui: WizardSleepUi,
    onEvent: (WizardEvent) -> Unit,
    copyFormatter: CopyLineFormatter? = null,
) {
    val haptics = LocalHaptics.current
    var handled by rememberSaveable { mutableIntStateOf(ui.continueFailures) }
    LaunchedEffect(ui.continueFailures) {
        if (ui.continueFailures > handled) haptics.perform(HapticEvent.Warning)
        handled = ui.continueFailures
    }
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PlanUITokens.wizardContentGap.dp),
    ) {
        WizardSleepCard {
            WizardTimeRow(
                WizardCopy("Usual bedtime"),
                ui.bed,
                ui.timeFormat,
                { onEvent(WizardEvent.OpenTimeSheet("habitual_bed")) },
                "wizard.sleep.habitual_bed",
            )
            HorizontalDivider(color = CfTheme.colors.border)
            WizardTimeRow(
                WizardCopy("Usual wake-up"),
                ui.wake,
                ui.timeFormat,
                { onEvent(WizardEvent.OpenTimeSheet("habitual_wake")) },
                "wizard.sleep.habitual_wake",
            )
        }
        Text(
            WizardCopy("When do you naturally feel most awake?").resolve(),
            Modifier.padding(
                top = PlanUITokens.space2.dp,
                start = PlanUITokens.space1.dp,
                end = PlanUITokens.space1.dp,
            ),
            color = CfTheme.colors.foreground,
            style = CfTheme.type.style(CfTypeScale.wizardButton),
        )
        WizardChronotypeGrid {
            listOf(
                Triple("early", "Early bird", "Up early, tired in the evening"),
                Triple("intermediate", "In between", "A normal working day"),
                Triple("late", "Night owl", "Late nights, slow mornings"),
                Triple("unsure", "Not sure", "We use your sleep times"),
            ).forEach { (key, title, hint) ->
                WizardChoiceCard(
                    WizardCopy(title),
                    WizardCopy(hint),
                    ui.selectedChronotype == key,
                    true,
                    "wizard.choice.sleep.$key",
                    {
                        haptics.perform(HapticEvent.Selection)
                        onEvent(
                            WizardEvent.SetSleep(
                                "chronotype",
                                if (key == "unsure") JsonNull else JsonPrimitive(key),
                            ),
                        )
                    },
                    style = WizardChoiceStyle.Chronotype,
                )
            }
        }
        ui.errors.forEach { (key, message) ->
            WizardErrorLine(fieldCopy(message, copyFormatter), "wizard.sleep.error.$key")
        }
    }
}

@Composable
internal fun WizardSleepCard(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(PlanUITokens.wizardCardRadius.dp)
    Surface(
        Modifier.fillMaxWidth().border(AuthTokens.borderWidth.dp, CfTheme.colors.border, shape),
        shape = shape,
        color = CfTheme.colors.card,
    ) { Column(verticalArrangement = Arrangement.Top) { content() } }
}
