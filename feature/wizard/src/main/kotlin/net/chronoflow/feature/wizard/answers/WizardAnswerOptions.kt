package net.chronoflow.feature.wizard.answers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfDivider
import net.chronoflow.core.designsystem.effect.CfPressIndication
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.haptics.HapticEvent
import net.chronoflow.core.designsystem.haptics.LocalHaptics
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardMetrics
import net.chronoflow.feature.wizard.ui.WizardCopy
import net.chronoflow.feature.wizard.ui.resolve

@Composable
internal fun WizardAnswerOptions(
    state: WizardAnswerSheetState,
    onEvent: (WizardEvent) -> Unit,
) {
    if (state.options.isEmpty()) return
    val haptics = LocalHaptics.current
    WizardAnswerCard {
        Column(Modifier.testTag("wizard.answer.options").selectableGroup()) {
            state.options.forEachIndexed { index, option ->
                if (index > 0) CfDivider()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = WizardMetrics.sheetRowMinHeight.dp)
                        .testTag("wizard.answer.option.$index")
                        .selectable(
                            state.isSelected(option),
                            interactionSource = null,
                            indication = CfPressIndication,
                            role = Role.RadioButton,
                        ) {
                            haptics.perform(HapticEvent.Selection)
                            onEvent(WizardEvent.UpdateAnswer(state.optionField, option.value))
                        }.padding(horizontal = PlanUITokens.space4.dp, vertical = PlanUITokens.space2.dp),
                    horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            WizardCopy(option.labelKey).resolve(),
                            color = CfTheme.colors.foreground,
                            style = CfTheme.type.style(CfTypeScale.wizardRow),
                        )
                        option.hintKey?.let {
                            Text(
                                WizardCopy(it).resolve(),
                                color = CfTheme.colors.mutedForeground,
                                style = CfTheme.type.style(CfTypeScale.wizardHint),
                            )
                        }
                    }
                    if (state.isSelected(option)) {
                        Icon(
                            LucideGlyphs.Check.imageVector(WizardMetrics.answerCheckStroke),
                            null,
                            Modifier.size(PlanUITokens.space5.dp),
                            tint = CfTheme.colors.dawnText,
                        )
                    }
                }
            }
        }
    }
}
