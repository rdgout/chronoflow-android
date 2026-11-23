package net.chronoflow.feature.trip.page

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import net.chronoflow.core.designsystem.component.CfButton
import net.chronoflow.core.designsystem.component.CfButtonSize
import net.chronoflow.core.designsystem.component.CfButtonStyle
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.ui.R

@Composable
fun HiddenStepsButton(
    onSettings: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    CfButton(
        stringResource(R.string.plan_some_steps_are_hidden_in_your_preferences),
        { onSettings?.invoke() },
        modifier.testTag("trip.hiddenSteps"),
        enabled = onSettings != null,
        style = CfButtonStyle.Ghost,
        size = CfButtonSize.Touch,
        fillWidth = true,
        textStyle = CfTheme.type.planListLine,
    )
}
