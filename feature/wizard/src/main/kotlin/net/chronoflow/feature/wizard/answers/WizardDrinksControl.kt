package net.chronoflow.feature.wizard.answers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.designsystem.component.CfDivider
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.haptics.HapticEvent
import net.chronoflow.core.designsystem.haptics.LocalHaptics
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.wizard.model.WizardOptionalAnswer
import net.chronoflow.feature.wizard.components.WizardStepper
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardMetrics
import net.chronoflow.feature.wizard.ui.WizardCopy
import net.chronoflow.feature.wizard.ui.resolve

@Composable
internal fun WizardDrinksControl(
    state: WizardAnswerSheetState,
    onEvent: (WizardEvent) -> Unit,
) {
    val haptics = LocalHaptics.current
    Column(verticalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp)) {
        Text(
            WizardCopy("Cups a day").resolve(),
            color = CfTheme.colors.mutedForeground,
            style = CfTheme.type.style(CfTypeScale.wizardHint),
        )
        Box(
            if (state.error == null) {
                Modifier
            } else {
                Modifier.testTag("wizard.answer.drinks.ring").answerRing(CfTheme.colors.destructive)
            },
        ) {
            WizardAnswerCard {
                androidx.compose.foundation.layout.Column(Modifier.testTag("wizard.answer.drinks")) {
                    WizardOptionalAnswer.drinkFields.forEachIndexed { index, field ->
                        if (index > 0) CfDivider()
                        val label = WizardCopy(listOf("Coffee", "Tea", "Soft drinks", "Energy drinks")[index]).resolve()
                        Row(
                            Modifier
                                .heightIn(min = WizardMetrics.sheetRowMinHeight.dp)
                                .padding(horizontal = PlanUITokens.space4.dp),
                            horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                label,
                                Modifier.weight(1f),
                                color = CfTheme.colors.foreground,
                                style = CfTheme.type.style(CfTypeScale.wizardRow),
                            )
                            Text(
                                state.count(field).toString(),
                                Modifier.widthIn(min = PlanUITokens.space5.dp),
                                color = CfTheme.colors.foreground,
                                textAlign = TextAlign.Center,
                                style = CfTheme.type.style(CfTypeScale.wizardButton).copy(fontFeatureSettings = "tnum"),
                            )
                            WizardStepper(state.count(field), 0..state.maxDrinks, label, {
                                haptics.perform(HapticEvent.Selection)
                                onEvent(WizardEvent.UpdateAnswer(field, JsonPrimitive(it)))
                            }, testTag = "wizard.answer.$field")
                        }
                    }
                }
            }
        }
    }
}
