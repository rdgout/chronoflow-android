@file:Suppress("ktlint:standard:enum-entry-name-case")

package net.chronoflow.core.wizard.generation

import kotlinx.serialization.Serializable
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.api.ApiTripReviewInput
import net.chronoflow.core.model.contracts.ApiReviewIdentity
import net.chronoflow.core.model.contracts.ApiTripInputSnapshot
import net.chronoflow.core.model.contracts.ApiTripReview

@Serializable
data class TripEditContext(
    val tripID: String,
    val sourceRevision: String,
)

data class TripGenerationReviewResult(
    val input: ApiTripReviewInput,
    val review: ApiTripReview,
    val edit: TripEditContext? = null,
    val sourceSnapshot: ApiTripInputSnapshot? = null,
)

@ConsistentCopyVisibility
data class TripGenerationState private constructor(
    val instance: String,
    val input: ApiTripReviewInput,
    val edit: TripEditContext?,
    val review: ApiTripReview?,
    val savedTripId: String?,
) {
    val identity: ApiReviewIdentity? get() = review?.identity

    companion object {
        operator fun invoke(
            instance: String,
            input: ApiTripReviewInput,
            edit: TripEditContext? = null,
            review: ApiTripReview? = null,
            savedTripId: String? = null,
        ): TripGenerationState =
            TripGenerationState(
                instance,
                input.copy(identity = review?.identity ?: input.identity),
                edit,
                review,
                savedTripId,
            )
    }
}

data class TripGenerationSaveResult(
    val state: TripGenerationState,
    val outcome: Outcome,
) {
    sealed interface Outcome {
        data class Saved(
            val tripId: String,
        ) : Outcome

        data class ReReviewNeeded(
            val reason: Reason,
        ) : Outcome

        data class Failed(
            val failure: DataFailure,
        ) : Outcome
    }

    enum class Reason { reviewExpired, notEnoughCredits }
}
