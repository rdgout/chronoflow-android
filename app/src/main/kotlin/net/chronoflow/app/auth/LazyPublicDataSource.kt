package net.chronoflow.app.auth

import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.MutationReceipt
import net.chronoflow.core.model.OutboxChange
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

internal class LazyPublicDataSource(
    private val source: suspend () -> DataSource,
) : DataSource {
    override suspend fun passkeyOptions(): ApiPasskeyChallenge = source().passkeyOptions()

    override suspend fun passkeyLogin(input: ApiPasskeyLoginInput): SessionResult = source().passkeyLogin(input)

    override suspend fun signIn(input: PasswordSignInInput): SignInResult = source().signIn(input)

    override suspend fun completeTwoFactor(input: TwoFactorInput): SessionResult = source().completeTwoFactor(input)

    override suspend fun signUp(input: RegisterInput): SessionResult = source().signUp(input)

    override suspend fun requestPasswordReset(email: String): Unit = source().requestPasswordReset(email)

    override suspend fun resetPassword(input: ResetPasswordInput): Unit = source().resetPassword(input)

    override suspend fun verificationStatus(): EmailVerificationStatus = source().verificationStatus()

    override suspend fun resendVerificationEmail(): EmailVerificationStatus = source().resendVerificationEmail()

    override suspend fun verifyEmail(input: SignedVerificationInput): EmailVerificationStatus =
        source().verifyEmail(input)

    override suspend fun signOut(): Unit = source().signOut()

    override suspend fun deleteAccount(password: String): Unit = source().deleteAccount(password)

    override suspend fun accountSecurity(change: AccountSecurityChange): AccountSecurityResult =
        source().accountSecurity(change)

    override suspend fun fetchIdentity(): AccountIdentity = source().fetchIdentity()

    override suspend fun fetchAccount(): AccountSnapshot = source().fetchAccount()

    override suspend fun fetchTrips(): List<TripListItem> = source().fetchTrips()

    override suspend fun fetchTripDetail(id: String): TripDetailResponse = source().fetchTripDetail(id)

    override suspend fun searchAirports(
        query: String,
        limit: Int?,
    ): List<ApiAirportSuggestion> = source().searchAirports(query, limit)

    override suspend fun reviewTrip(input: ApiTripReviewInput): ApiTripReview = source().reviewTrip(input)

    override suspend fun reviewGuestTrip(input: ApiGuestTripReviewInput): ApiTripReview =
        source().reviewGuestTrip(input)

    override suspend fun reviewTripEdit(
        input: ApiTripReviewInput,
        id: String,
        sourceRevision: String,
    ): ApiTripReview = source().reviewTripEdit(input, id, sourceRevision)

    override suspend fun saveTrip(identity: ApiTripSaveInput): ApiTripSaveResult = source().saveTrip(identity)

    override suspend fun updateTrip(
        id: String,
        identity: ApiTripSaveInput,
        sourceRevision: String,
    ): ApiTripSaveResult = source().updateTrip(id, identity, sourceRevision)

    override suspend fun tripInput(id: String): ApiTripInputSnapshot = source().tripInput(id)

    override suspend fun send(change: OutboxChange): MutationReceipt = source().send(change)
}
