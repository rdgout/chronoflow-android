package net.chronoflow.feature.wizard.finetune

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfDivider
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.system.CfSwitch
import net.chronoflow.core.designsystem.system.cfToggleable
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.sleep.WizardSleepCard
import net.chronoflow.feature.wizard.ui.WizardCopy
import net.chronoflow.feature.wizard.ui.resolve

@Composable
internal fun WizardPreferencesCard(
    ui: WizardFineTuneUi,
    onEvent: (WizardEvent) -> Unit,
) {
    WizardSleepCard {
        Column(Modifier.testTag("wizard.preferences")) {
            PreferenceRow(
                "exclude_caffeine",
                "Leave caffeine out of my plan",
                "Your plan won't suggest any caffeine.",
                ui,
                onEvent,
            )
            CfDivider()
            PreferenceRow(
                "flight_movement_breaks",
                "Add movement breaks during long-flight sleep",
                "Your plan breaks up long sleep on long flights with short movement breaks. If you're at higher risk of blood clots, choose this and ask your doctor about compression stockings and an aisle seat.",
                ui,
                onEvent,
            )
        }
    }
}

@Composable
private fun PreferenceRow(
    field: String,
    title: String,
    hint: String,
    ui: WizardFineTuneUi,
    onEvent: (WizardEvent) -> Unit,
) {
    val checked = ui.preferences[field] == true
    Row(
        Modifier
            .testTag("wizard.preference.$field")
            .cfToggleable(checked, {
                onEvent(WizardEvent.SetPreference(field, it))
            }, enabled = !ui.reviewing)
            .heightIn(min = PlanUITokens.wizardButtonHeight.dp)
            .padding(PlanUITokens.space4.dp),
        horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PlanUITokens.wizardLabelGap.dp)) {
            Text(
                WizardCopy(title).resolve(),
                color = CfTheme.colors.foreground,
                style = CfTheme.type.style(CfTypeScale.wizardChronotype),
            )
            Text(
                WizardCopy(hint).resolve(),
                color = CfTheme.colors.mutedForeground,
                style = CfTheme.type.style(CfTypeScale.wizardHint),
            )
        }
        CfSwitch(checked, null)
    }
}
