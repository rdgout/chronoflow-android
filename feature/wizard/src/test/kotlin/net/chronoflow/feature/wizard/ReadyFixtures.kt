package net.chronoflow.feature.wizard

import net.chronoflow.core.model.contracts.ApiTripReview
import net.chronoflow.core.testing.wizard.generationReview
import net.chronoflow.core.wizard.model.MemberWizardSnapshot
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.feature.wizard.ready.ReadyUiMapper

internal fun readyActive(
    review: ApiTripReview? = generationReview(),
    reviewing: Boolean = false,
    retry: Int = 0,
): TripWizardUiState.Active {
    val base = fineTuneDraft()
    return TripWizardUiState.Active(
        MemberWizardSnapshot(
            base.copy(state = base.state.copy(step = MemberWizardStep.ready, review = review)),
            reviewing = reviewing,
            retrySeconds = retry,
        ),
        .95,
        emptyList(),
        optionalPrompts = review?.prompts.orEmpty(),
    )
}

internal fun TripWizardUiState.Active.ready() = ReadyUiMapper.map(this)!!
