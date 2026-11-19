package net.chronoflow.core.datamock

import kotlinx.coroutines.Dispatchers
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.api.ApiGuestTripReviewInput
import net.chronoflow.core.model.api.RegisterInput
import net.chronoflow.core.model.contracts.ApiTripInput
import net.chronoflow.core.model.contracts.ApiTripInputLeg
import net.chronoflow.core.model.contracts.ApiTripReview

internal fun guestMockSource(
    clock: MockClock = MockClock(),
    store: MockStateStore = MemoryMockStateStore(),
): MockDataSource =
    MockDataSource(
        store,
        MockServerState(),
        now = clock::now,
        sleep = clock::sleep,
        id = clock::id,
        zone = mockZone,
        io = Dispatchers.Unconfined,
    )

internal fun continuedGuestInput(review: ApiTripReview): ApiGuestTripReviewInput =
    guestReviewInput().copy(identity = review.identity, guestSecret = review.identity.guestSecret)

internal fun guestRegistration(review: ApiTripReview): RegisterInput =
    registration(
        "traveller@example.com",
    ).copy(
        deviceID = guestReviewInput().deviceID,
        draftToken = review.identity.token,
        inputRevision = review.identity.inputRevision,
        reviewRevision = review.identity.reviewRevision,
        guestSecret = review.identity.guestSecret,
    )

internal val expiredGuestClaim: DataFailure get() =
    DataFailure.Validation(
        mapOf("review_expired" to FieldMessage("Review your trip again before creating your account.")),
        422,
    )

internal fun ApiTripInput.firstLeg(change: (ApiTripInputLeg) -> ApiTripInputLeg): ApiTripInput =
    copy(
        outbound =
            outbound.copy(
                legs =
                    listOf(change(outbound.legs.first())) + outbound.legs.drop(1),
            ),
    )
