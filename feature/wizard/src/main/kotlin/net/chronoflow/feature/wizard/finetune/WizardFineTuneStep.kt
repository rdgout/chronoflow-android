package net.chronoflow.feature.wizard.finetune

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
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
    viewport: WizardFineTuneViewport? = null,
) {
    Column(
        Modifier.fillMaxWidth().testTag("wizard.fine-tune"),
        verticalArrangement = Arrangement.spacedBy(PlanUITokens.wizardContentGap.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().layout { measurable, constraints ->
                val gap = PlanUITokens.wizardContentGap.dp.roundToPx()
                val minimum = viewport?.let { (it.height - it.heading - gap).coerceAtLeast(0) } ?: 0
                val content = measurable.measure(constraints.copy(minHeight = 0))
                val height = maxOf(content.height, minimum).coerceAtMost(constraints.maxHeight)
                layout(content.width, height) { content.placeRelative(0, 0) }
            },
            verticalArrangement = Arrangement.spacedBy(PlanUITokens.wizardContentGap.dp),
        ) {
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
    }
}
