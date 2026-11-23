package net.chronoflow.feature.wizard.shell

import androidx.compose.runtime.Composable

@Composable
internal fun WizardStepSeam(key: WizardStepKey) {
    net.chronoflow.feature.wizard.debug.WizardStepSeam
        .Render(key)
}
