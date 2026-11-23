package net.chronoflow.core.wizard.generation

import kotlinx.coroutines.ensureActive
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.api.ApiTripReviewInput
import net.chronoflow.core.model.contracts.ApiTripInputSnapshot
import net.chronoflow.core.model.contracts.ApiTripReview
import kotlin.coroutines.coroutineContext

private data class ReviewRequest(
    val input: ApiTripReviewInput,
    val edit: TripEditContext?,
    val snapshot: ApiTripInputSnapshot? = null,
)

internal suspend fun performReview(
    source: DataSource,
    input: ApiTripReviewInput,
    edit: TripEditContext?,
): TripGenerationReviewResult {
    var request = refreshSource(ReviewRequest(input, edit), source)
    var review =
        try {
            sendReview(request, source)
        } catch (_: DataFailure.ReviewExpired) {
            request = refreshSource(request, source).let { it.copy(input = it.input.copy(identity = null)) }
            sendReview(request, source)
        }
    if (request.edit != null && review.identity.sourceRevision != request.edit.sourceRevision) {
        request = refreshSource(request, source)
        review = sendReview(request, source)
        if (review.identity.sourceRevision != request.edit?.sourceRevision) throw DataFailure.ReviewExpired
    }
    return TripGenerationReviewResult(
        request.input.copy(identity = review.identity),
        review,
        request.edit,
        request.snapshot,
    )
}

private suspend fun refreshSource(
    original: ReviewRequest,
    source: DataSource,
): ReviewRequest {
    coroutineContext.ensureActive()
    val edit = original.edit ?: return original
    val snapshot = source.tripInput(edit.tripID)
    coroutineContext.ensureActive()
    if (snapshot.sourceRevision == edit.sourceRevision) return original
    return ReviewRequest(
        original.input.copy(input = snapshot.input, identity = null, profile = null),
        TripEditContext(edit.tripID, snapshot.sourceRevision),
        snapshot,
    )
}

private suspend fun sendReview(
    request: ReviewRequest,
    source: DataSource,
): ApiTripReview {
    coroutineContext.ensureActive()
    val edit = request.edit
    return if (edit ==
        null
    ) {
        source.reviewTrip(request.input)
    } else {
        source.reviewTripEdit(request.input, edit.tripID, edit.sourceRevision)
    }
}
