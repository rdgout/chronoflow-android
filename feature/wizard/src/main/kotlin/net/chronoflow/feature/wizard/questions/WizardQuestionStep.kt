package net.chronoflow.feature.wizard.questions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.haptics.HapticEvent
import net.chronoflow.core.designsystem.haptics.LocalHaptics
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.feature.wizard.components.WizardChoiceCard
import net.chronoflow.feature.wizard.components.WizardChoiceStyle
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.ui.WizardCopy
import net.chronoflow.feature.wizard.ui.resolve

@Composable
internal fun WizardQuestionStep(
    ui: WizardQuestionUi,
    onEvent: (WizardEvent) -> Unit,
) {
    val haptics = LocalHaptics.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(PlanUITokens.wizardContentGap.dp)) {
        Column(
            Modifier.selectableGroup().testTag("wizard.question.options"),
            verticalArrangement = Arrangement.spacedBy(PlanUITokens.wizardChoiceGap.dp),
        ) {
            ui.options.forEach { option ->
                WizardChoiceCard(
                    option.title ?: WizardCopy(""),
                    null,
                    ui.selected == option.value,
                    ui.enabled && !ui.pending,
                    option.tag,
                    {
                        haptics.perform(HapticEvent.Selection)
                        onEvent(WizardEvent.AnswerQuestion(option.value))
                    },
                    style = WizardChoiceStyle.Question,
                    rawTitle = option.rawTitle,
                )
            }
        }
        ui.why?.let {
            Text(
                it.resolve(),
                Modifier
                    .padding(top = PlanUITokens.space1.dp)
                    .padding(horizontal = PlanUITokens.space1.dp)
                    .testTag("wizard.question.why"),
                style = CfTheme.type.style(CfTypeScale.wizardHint),
                color = CfTheme.colors.mutedForeground,
            )
        }
        if (ui.disclaimer) {
            Column(
                Modifier
                    .padding(top = PlanUITokens.wizardLabelGap.dp)
                    .padding(horizontal = PlanUITokens.space1.dp),
            ) {
                HorizontalDivider(thickness = AuthTokens.borderWidth.dp, color = CfTheme.colors.border)
                Spacer(Modifier.height(PlanUITokens.wizardChoiceGap.dp))
                Text(
                    WizardCopy(
                        "ChronoFlow gives timing guidance, not medical advice. If you're unsure about a question, check with your doctor.",
                    ).resolve(),
                    Modifier.testTag("wizard.question.disclaimer"),
                    style = CfTheme.type.style(CfTypeScale.wizardHint),
                    color = CfTheme.colors.mutedForeground,
                )
            }
        }
    }
}
