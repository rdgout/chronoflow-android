package net.chronoflow.core.wizard.model

import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.api.ApiTripReviewInput
import net.chronoflow.core.wizard.generation.MemberWizardGeneration
import net.chronoflow.core.wizard.generation.TripEditContext
import net.chronoflow.core.wizard.generation.TripGenerationReviewResult
import net.chronoflow.core.wizard.generation.TripGenerationSaveResult
import net.chronoflow.core.wizard.generation.TripGenerationState

class FakeWizardGeneration : MemberWizardGeneration {
    val reviews = ArrayDeque<Result<TripGenerationReviewResult>>()
    val outcomes = ArrayDeque<TripGenerationSaveResult.Outcome>()
    val inputs = mutableListOf<ApiTripReviewInput>()
    var saves = 0
    val cancellations = mutableListOf<String>()
    var cancelled = false
    var reviewHandler: (suspend () -> TripGenerationReviewResult)? = null
    var saveFailure: Throwable? = null

    override suspend fun review(
        instance: String,
        input: ApiTripReviewInput,
        edit: TripEditContext?,
    ): TripGenerationReviewResult {
        inputs.add(input)
        reviewHandler?.let { return it() }
        return reviews.removeFirstOrNull()?.getOrThrow() ?: throw DataFailure.Server(null)
    }

    override suspend fun save(
        state: TripGenerationState,
        didCreate: suspend (String) -> Unit,
    ): TripGenerationSaveResult {
        saves++
        saveFailure?.let { throw it }
        val outcome = outcomes.removeFirstOrNull() ?: TripGenerationSaveResult.Outcome.Failed(DataFailure.Server(null))
        if (outcome is TripGenerationSaveResult.Outcome.Saved) {
            didCreate(outcome.tripId)
            return TripGenerationSaveResult(
                TripGenerationState(state.instance, state.input, state.edit, state.review, outcome.tripId),
                outcome,
            )
        }
        return TripGenerationSaveResult(state, outcome)
    }

    override suspend fun cancelReviews(instance: String) {
        cancellations.add(instance)
    }

    override suspend fun cancel() {
        cancelled = true
    }
}
