package net.chronoflow.feature.wizard.ready

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfButton
import net.chronoflow.core.designsystem.component.CfButtonStyle
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.ui.WizardCopy
import net.chronoflow.feature.wizard.ui.resolve

@Composable
internal fun ReadyFooter(
    enabled: Boolean,
    onEvent: (WizardEvent) -> Unit,
) {
    CfButton(
        WizardCopy("Close").resolve(),
        { onEvent(WizardEvent.CloseFromFooter) },
        Modifier.testTag("wizard.ready.no-credits"),
        style = CfButtonStyle.Ghost,
        fillWidth = true,
        minHeight = PlanUITokens.actionMinHeight.dp,
        textStyle = CfTheme.type.style(CfTypeScale.wizardSecondary),
        enabled = enabled,
    )
}
