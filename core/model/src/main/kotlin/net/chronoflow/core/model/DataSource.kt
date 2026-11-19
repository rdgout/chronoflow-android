package net.chronoflow.core.model

import net.chronoflow.core.model.api.AccountIdentity
import net.chronoflow.core.model.api.AccountSecurityChange
import net.chronoflow.core.model.api.AccountSecurityResult
import net.chronoflow.core.model.api.AccountSnapshot
import net.chronoflow.core.model.api.ApiGuestTripReviewInput
import net.chronoflow.core.model.api.ApiPasskeyChallenge
import net.chronoflow.core.model.api.ApiPasskeyLoginInput
import net.chronoflow.core.model.api.ApiTripReviewInput
import net.chronoflow.core.model.api.ApiTripSaveInput
import net.chronoflow.core.model.api.EmailVerificationStatus
import net.chronoflow.core.model.api.PasswordSignInInput
import net.chronoflow.core.model.api.RegisterInput
import net.chronoflow.core.model.api.ResetPasswordInput
import net.chronoflow.core.model.api.SessionResult
import net.chronoflow.core.model.api.SignInResult
import net.chronoflow.core.model.api.SignedVerificationInput
import net.chronoflow.core.model.api.TwoFactorInput
import net.chronoflow.core.model.contracts.ApiAirportSuggestion
import net.chronoflow.core.model.contracts.ApiTripInputSnapshot
import net.chronoflow.core.model.contracts.ApiTripReview
import net.chronoflow.core.model.contracts.ApiTripSaveResult
import net.chronoflow.core.model.contracts.TripDetailResponse
import net.chronoflow.core.model.contracts.TripListItem

interface DataSource {
    suspend fun passkeyOptions(): ApiPasskeyChallenge

    suspend fun passkeyLogin(input: ApiPasskeyLoginInput): SessionResult

    suspend fun signIn(input: PasswordSignInInput): SignInResult

    suspend fun completeTwoFactor(input: TwoFactorInput): SessionResult

    suspend fun signUp(input: RegisterInput): SessionResult

    suspend fun requestPasswordReset(email: String)

    suspend fun resetPassword(input: ResetPasswordInput)

    suspend fun verificationStatus(): EmailVerificationStatus

    suspend fun resendVerificationEmail(): EmailVerificationStatus

    suspend fun verifyEmail(input: SignedVerificationInput): EmailVerificationStatus

    suspend fun signOut()

    suspend fun deleteAccount(password: String): Unit = throw DataFailure.Unsupported(OutboxKind.Security)

    suspend fun accountSecurity(change: AccountSecurityChange): AccountSecurityResult =
        throw DataFailure.Unsupported(OutboxKind.Security)

    suspend fun fetchIdentity(): AccountIdentity

    suspend fun fetchAccount(): AccountSnapshot

    suspend fun fetchTrips(): List<TripListItem>

    suspend fun fetchTripDetail(id: String): TripDetailResponse

    suspend fun searchAirports(
        query: String,
        limit: Int?,
    ): List<ApiAirportSuggestion>

    suspend fun reviewTrip(input: ApiTripReviewInput): ApiTripReview

    suspend fun reviewGuestTrip(input: ApiGuestTripReviewInput): ApiTripReview =
        throw DataFailure.Unsupported(OutboxKind.Profile)

    suspend fun reviewTripEdit(
        input: ApiTripReviewInput,
        id: String,
        sourceRevision: String,
    ): ApiTripReview

    suspend fun saveTrip(identity: ApiTripSaveInput): ApiTripSaveResult

    suspend fun updateTrip(
        id: String,
        identity: ApiTripSaveInput,
        sourceRevision: String,
    ): ApiTripSaveResult

    suspend fun tripInput(id: String): ApiTripInputSnapshot

    suspend fun send(change: OutboxChange): MutationReceipt
}
