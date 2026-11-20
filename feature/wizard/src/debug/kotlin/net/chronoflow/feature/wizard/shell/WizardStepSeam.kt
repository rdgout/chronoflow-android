package net.chronoflow.feature.wizard.shell

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import net.chronoflow.core.ui.R

@Composable
internal fun WizardStepSeam(key: WizardStepKey) {
    Text(stringResource(R.string.app_debug_route_x, key.debugName), Modifier.testTag("wizard.seam.${key.debugName}"))
}
