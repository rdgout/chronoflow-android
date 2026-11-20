package net.chronoflow.feature.wizard

import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.wizard.model.MemberWizardDestination
import net.chronoflow.core.wizard.model.MemberWizardSnapshot

sealed interface TripWizardUiState {
    data object Loading : TripWizardUiState

    data class Active(
        val wizard: MemberWizardSnapshot,
        val progress: Double,
        val path: List<MemberWizardDestination>,
    ) : TripWizardUiState

    data class LoadFailed(
        val failure: DataFailure,
    ) : TripWizardUiState

    data class Closed(
        val createdTripId: String?,
    ) : TripWizardUiState
}
