package net.chronoflow.feature.wizard.answers

import net.chronoflow.core.model.contracts.ApiReviewPrompt
import net.chronoflow.core.model.contracts.PromptKind

internal object WizardPromptLabels {
    fun key(prompt: ApiReviewPrompt): String =
        when (prompt.kind) {
            PromptKind.SeatSide -> "Side of the plane"
            PromptKind.Room -> "Dark room at your stay"
            PromptKind.LateCheckOut -> "Late check-out"
            PromptKind.LatestWake -> "Latest you can wake up"
            PromptKind.PreDepartureBusyHours -> "Busy hours before you leave"
            PromptKind.Cups -> "Coffee, tea and soft drinks"
            PromptKind.LightDevice -> "Light box or light glasses"
            PromptKind.BlueBlockers -> "Blue-blocking glasses"
            PromptKind.FreeDayTimes -> "Sleep on free days"
            else -> prompt.text.key
        }
}
