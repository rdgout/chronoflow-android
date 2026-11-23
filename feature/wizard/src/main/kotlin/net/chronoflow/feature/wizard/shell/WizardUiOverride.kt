package net.chronoflow.feature.wizard.shell

import net.chronoflow.core.wizard.model.WizardCheckingProgress

data class WizardUiOverride(
    val checking: WizardCheckingProgress? = null,
    val retrySeconds: Int? = null,
    val reviewing: Boolean? = null,
    val answerError: String? = null,
)
