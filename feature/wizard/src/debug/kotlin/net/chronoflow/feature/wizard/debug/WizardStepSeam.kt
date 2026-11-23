package net.chronoflow.feature.wizard.debug

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import net.chronoflow.core.ui.R
import net.chronoflow.feature.wizard.shell.WizardStepKey

internal object WizardStepSeam {
    @Composable
    fun Render(
        key: WizardStepKey,
        modifier: Modifier = Modifier,
    ) {
        Text(
            stringResource(R.string.app_debug_route_x, key.debugName),
            modifier.testTag("wizard.seam.${key.debugName}"),
        )
    }
}
