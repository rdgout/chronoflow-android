package net.chronoflow.feature.trip.actions

import net.chronoflow.core.plan.actions.TripAction

fun interface TripWizardPreparer {
    suspend fun prepare(
        tripId: String,
        action: TripAction,
    ): TripWizardPrepareOutcome
}

sealed interface TripWizardPrepareOutcome {
    data object Ready : TripWizardPrepareOutcome

    data object Ignored : TripWizardPrepareOutcome

    data class Failed(
        val offline: Boolean,
    ) : TripWizardPrepareOutcome
}
