package net.chronoflow.feature.wizard.answers

import net.chronoflow.core.model.contracts.ApiReviewPrompt
import net.chronoflow.core.model.contracts.PromptKind

internal object WizardPromptLabels {
    fun key(prompt: ApiReviewPrompt): String =
        when (prompt.kind) {
            PromptKind.FreeDayTimes -> "Sleep on free days"
            else -> prompt.text.key
        }
}
