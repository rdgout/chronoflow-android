package net.chronoflow.core.model.contracts

import assertk.assertFailure
import assertk.assertions.isInstanceOf
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import net.chronoflow.core.model.api.AccountSnapshot
import net.chronoflow.core.model.api.ApiErrorEnvelope
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.api.ApiPasskeyChallenge
import net.chronoflow.core.model.api.ApiTripSaveInput
import net.chronoflow.core.model.api.EmailVerificationStatus
import net.chronoflow.core.model.api.SessionAccount
import net.chronoflow.core.model.api.SignInResult

private fun <T : Any> decoder(serializer: DeserializationStrategy<T>): (ByteArray) -> Any =
    { bytes -> ApiJson.response.decodeFromString(serializer, bytes.decodeToString()) }

private val errorEnvelope = decoder(ApiErrorEnvelope.serializer())
private val plan = decoder(Parts.serializer())
private val signIn = decoder(SignInResult.serializer())
private val tripInput = decoder(ApiTripInput.serializer())
private val review = decoder(ApiTripReview.serializer())

// guest-review-request.json has no `override`, which is required and not nullable on the wire.
private val guestRequest: (ByteArray) -> Any = { bytes ->
    assertFailure { ApiJson.response.decodeFromString(ApiTripInput.serializer(), bytes.decodeToString()) }
        .isInstanceOf<SerializationException>()
}

internal val API_FIXTURE_DECODERS: Map<String, (ByteArray) -> Any> =
    mapOf(
        "account/identity.json" to decoder(SessionAccount.serializer()),
        "account/snapshot.json" to decoder(AccountSnapshot.serializer()),
        "auth/passkey-options.json" to decoder(ApiPasskeyChallenge.serializer()),
        "auth/session.json" to signIn,
        "auth/two-factor.json" to signIn,
        "auth/unverified.json" to signIn,
        "auth/verification.json" to decoder(EmailVerificationStatus.serializer()),
        "errors/conflict.json" to errorEnvelope,
        "errors/email_unverified.json" to errorEnvelope,
        "errors/forbidden.json" to errorEnvelope,
        "errors/http_error.json" to errorEnvelope,
        "errors/method_not_allowed.json" to errorEnvelope,
        "errors/not_found.json" to errorEnvelope,
        "errors/payload_too_large.json" to errorEnvelope,
        "errors/server_error.json" to errorEnvelope,
        "errors/throttled.json" to errorEnvelope,
        "errors/unauthenticated.json" to errorEnvelope,
        "errors/unavailable.json" to errorEnvelope,
        "errors/validation_failed.json" to errorEnvelope,
        "errors/wrong_credentials.json" to errorEnvelope,
        "plans/S1.json" to plan,
        "plans/S2.json" to plan,
        "plans/S3.json" to plan,
        "plans/S4.json" to plan,
        "plans/S5.json" to plan,
        "plans/S6.json" to plan,
        "plans/S6-RETURN-10N.json" to plan,
        "trips/checks.json" to decoder(TripCheckResult.serializer()),
        "trips/detail.json" to { bytes -> TripDetailResponse.decode(bytes) },
        "trips/list.json" to decoder(TripListResponse.serializer()),
        "wizard/airports.json" to decoder(ApiAirportSearchResults.serializer()),
        "wizard/capacity.json" to errorEnvelope,
        "wizard/credits.json" to errorEnvelope,
        "wizard/expired.json" to errorEnvelope,
        "wizard/journey.json" to errorEnvelope,
        "wizard/profile.json" to errorEnvelope,
        "wizard/throttled.json" to errorEnvelope,
        "wizard/unknown-field.json" to errorEnvelope,
        "wizard/input.json" to tripInput,
        "wizard/review-request.json" to tripInput,
        "wizard/guest-review-request.json" to guestRequest,
        "wizard/needs-answers.json" to review,
        "wizard/review.json" to review,
        "wizard/save-request.json" to decoder(ApiTripSaveInput.serializer()),
        "wizard/save.json" to decoder(ApiTripSaveResult.serializer()),
        "wizard/snapshot.json" to decoder(ApiTripInputSnapshot.serializer()),
    )
