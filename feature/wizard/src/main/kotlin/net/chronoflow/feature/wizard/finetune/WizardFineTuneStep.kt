package net.chronoflow.feature.wizard.finetune

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfDivider
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.sleep.WizardSleepCard

@Composable
internal fun WizardFineTuneStep(
    ui: WizardFineTuneUi,
    formatter: CopyLineFormatter?,
    onEvent: (WizardEvent) -> Unit,
    modifier: Modifier = Modifier,
    heading: @Composable () -> Unit = {},
) {
    Box(modifier, propagateMinConstraints = true) {
        Layout(
            modifier = Modifier.fillMaxWidth().testTag("wizard.fine-tune"),
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(PlanUITokens.wizardContentGap.dp)) {
                    heading()
                    if (!ui.none) {
                        WizardSleepCard {
                            Column(Modifier.testTag("wizard.extras")) {
                                ui.rows.forEachIndexed { i, row ->
                                    key(row.promptId) {
                                        if (i > 0) CfDivider()
                                        WizardExtraRow(row, !ui.reviewing, formatter, onEvent)
                                    }
                                }
                            }
                        }
                    }
                    WizardPreparationCard(ui, formatter, onEvent)
                }
                WizardPreferencesCard(ui, onEvent)
            },
        ) { measurables, constraints ->
            val contentConstraints = constraints.copy(minHeight = 0)
            val cards = measurables[0].measure(contentConstraints)
            val preferences = measurables[1].measure(contentConstraints)
            val gap = PlanUITokens.wizardContentGap.dp.roundToPx()
            val preferencesY = maxOf(cards.height, constraints.minHeight) + gap
            layout(
                constraints.constrainWidth(maxOf(cards.width, preferences.width)),
                constraints.constrainHeight(preferencesY + preferences.height),
            ) {
                cards.placeRelative(0, 0)
                preferences.placeRelative(0, preferencesY)
            }
        }
    }
}
