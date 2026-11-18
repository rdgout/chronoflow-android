package net.chronoflow.core.model

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.api.AccountIdentity
import net.chronoflow.core.model.api.AccountSecurityChange
import net.chronoflow.core.model.api.AccountSnapshot
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
import net.chronoflow.core.model.api.guestReviewInput
import net.chronoflow.core.model.contracts.ApiAirportSuggestion
import net.chronoflow.core.model.contracts.ApiTripInputSnapshot
import net.chronoflow.core.model.contracts.ApiTripReview
import net.chronoflow.core.model.contracts.ApiTripSaveResult
import net.chronoflow.core.model.contracts.TripDetailResponse
import net.chronoflow.core.model.contracts.TripListItem
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class DataSourceContractTests {
    @Test fun hasTheTwentyFiveIosMethods() {
        val names =
            DataSource::class.java.declaredMethods
                .filterNot { it.isSynthetic }
                .map { it.name.removeSuffix("\$default") }
                .distinct()
                .toSet()
        assertThat(
            names,
        ).isEqualTo(
            setOf(
                "passkeyOptions",
                "passkeyLogin",
                "signIn",
                "completeTwoFactor",
                "signUp",
                "requestPasswordReset",
                "resetPassword",
                "verificationStatus",
                "resendVerificationEmail",
                "verifyEmail",
                "signOut",
                "deleteAccount",
                "accountSecurity",
                "fetchIdentity",
                "fetchAccount",
                "fetchTrips",
                "fetchTripDetail",
                "searchAirports",
                "reviewTrip",
                "reviewGuestTrip",
                "reviewTripEdit",
                "saveTrip",
                "updateTrip",
                "tripInput",
                "send",
            ),
        )
    }

    @Test fun defaultsThrowUnsupported() =
        runTest(timeout = 60.seconds) {
            val source = UnusedDataSource()
            assertThat(failure { source.deleteAccount("x") }).isEqualTo(DataFailure.Unsupported(OutboxKind.Security))
            assertThat(failure { source.accountSecurity(AccountSecurityChange.ListPasskeys) })
                .isEqualTo(DataFailure.Unsupported(OutboxKind.Security))
            assertThat(failure { source.reviewGuestTrip(guestReviewInput()) })
                .isEqualTo(DataFailure.Unsupported(OutboxKind.Profile))
        }

    private suspend fun failure(block: suspend () -> Unit): DataFailure =
        try {
            block()
            error("expected DataFailure")
        } catch (failure: DataFailure) {
            failure
        }
}

private class UnusedDataSource : DataSource {
    override suspend fun passkeyOptions(): ApiPasskeyChallenge = error("unused")

    override suspend fun passkeyLogin(input: ApiPasskeyLoginInput): SessionResult = error("unused")

    override suspend fun signIn(input: PasswordSignInInput): SignInResult = error("unused")

    override suspend fun completeTwoFactor(input: TwoFactorInput): SessionResult = error("unused")

    override suspend fun signUp(input: RegisterInput): SessionResult = error("unused")

    override suspend fun requestPasswordReset(email: String) = error("unused")

    override suspend fun resetPassword(input: ResetPasswordInput) = error("unused")

    override suspend fun verificationStatus(): EmailVerificationStatus = error("unused")

    override suspend fun resendVerificationEmail(): EmailVerificationStatus = error("unused")

    override suspend fun verifyEmail(input: SignedVerificationInput): EmailVerificationStatus = error("unused")

    override suspend fun signOut() = error("unused")

    override suspend fun fetchIdentity(): AccountIdentity = error("unused")

    override suspend fun fetchAccount(): AccountSnapshot = error("unused")

    override suspend fun fetchTrips(): List<TripListItem> = error("unused")

    override suspend fun fetchTripDetail(id: String): TripDetailResponse = error("unused")

    override suspend fun searchAirports(
        query: String,
        limit: Int?,
    ): List<ApiAirportSuggestion> = error("unused")

    override suspend fun reviewTrip(input: ApiTripReviewInput): ApiTripReview = error("unused")

    override suspend fun reviewTripEdit(
        input: ApiTripReviewInput,
        id: String,
        sourceRevision: String,
    ): ApiTripReview = error("unused")

    override suspend fun saveTrip(identity: ApiTripSaveInput): ApiTripSaveResult = error("unused")

    override suspend fun updateTrip(
        id: String,
        identity: ApiTripSaveInput,
        sourceRevision: String,
    ): ApiTripSaveResult = error("unused")

    override suspend fun tripInput(id: String): ApiTripInputSnapshot = error("unused")

    override suspend fun send(change: OutboxChange): MutationReceipt = error("unused")
}
