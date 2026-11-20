@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.feature.wizard.returnchoice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.haptics.HapticEvent
import net.chronoflow.core.designsystem.haptics.LocalHaptics
import net.chronoflow.feature.wizard.components.WizardChoiceCard
import net.chronoflow.feature.wizard.shell.WizardEvent

@Composable
internal fun WizardReturnStep(
    ui: WizardReturnUi,
    onEvent: (WizardEvent) -> Unit,
) {
    val haptics = LocalHaptics.current
    Column(
        Modifier.fillMaxWidth().selectableGroup().testTag("wizard.return.choices"),
        verticalArrangement = Arrangement.spacedBy(PlanUITokens.wizardChoiceGap.dp),
    ) {
        val one = ui.oneWay
        WizardChoiceCard(one.title, one.hint, one.selected, one.enabled, "wizard.choice.one-way", {
            haptics.perform(HapticEvent.Selection)
            onEvent(WizardEvent.ChooseReturn(one.kind))
        })
        val home = ui.addReturn
        WizardChoiceCard(home.title, home.hint, home.selected, home.enabled, "wizard.choice.add-return", {
            haptics.perform(HapticEvent.Selection)
            onEvent(WizardEvent.ChooseReturn(home.kind))
        })
    }
}
