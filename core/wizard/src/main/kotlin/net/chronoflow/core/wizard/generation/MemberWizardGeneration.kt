package net.chronoflow.core.wizard.generation

import net.chronoflow.core.model.api.ApiTripReviewInput

interface MemberWizardGeneration {
    suspend fun review(
        instance: String,
        input: ApiTripReviewInput,
        edit: TripEditContext? = null,
    ): TripGenerationReviewResult

    suspend fun save(
        state: TripGenerationState,
        didCreate: suspend (String) -> Unit,
    ): TripGenerationSaveResult

    suspend fun cancelReviews(instance: String)

    suspend fun cancel()
}
