package net.chronoflow.feature.wizard.shell

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

internal fun Modifier.wizardBuiltStepBoundary(key: WizardStepKey): Modifier =
    if (key == WizardStepKey.Outbound) testTag("wizard.seam.outbound") else this
