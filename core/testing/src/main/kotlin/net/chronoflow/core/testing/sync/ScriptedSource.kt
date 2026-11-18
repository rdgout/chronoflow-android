package net.chronoflow.core.testing.sync

import net.chronoflow.core.model.DataFailure
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

class ScriptedSource : DataSource {
    val calls = mutableListOf<String>()
    private var activeCalls = 0
    var maximumActiveCalls = 0
        private set
    val changes = mutableListOf<OutboxChange>()
    private var failures: List<DataFailure?> = emptyList()
    var identityValue = identity()
        private set
    var snapshotValue = snapshot()
        private set
    var tripValues = listOf(card())
        private set
    var detailValue = detail()
        private set
    private var failMethod: String? = null
    private var methodFailure: DataFailure = DataFailure.Offline
    private var receipts: List<MutationReceipt> = emptyList()
    private var hook: (suspend (String) -> Unit)? = null

    fun configure(
        identity: AccountIdentity? = null,
        snapshot: AccountSnapshot? = null,
        trips: List<TripListItem>? = null,
        detail: net.chronoflow.core.model.contracts.TripDetail? = null,
        failures: List<DataFailure?> = emptyList(),
        failMethod: String? = null,
        methodFailure: DataFailure = DataFailure.Offline,
        receipts: List<MutationReceipt> = emptyList(),
        hook: (suspend (String) -> Unit)? = null,
    ) {
        identity?.let { identityValue = it }
        snapshot?.let { snapshotValue = it }
        trips?.let { tripValues = it }
        detail?.let { detailValue = it }
        this.failures = failures
        this.failMethod = failMethod
        this.methodFailure = methodFailure
        this.receipts = receipts
        this.hook = hook
    }

    private suspend fun record(method: String) {
        calls += method
        activeCalls++
        maximumActiveCalls = maxOf(maximumActiveCalls, activeCalls)
        try {
            hook?.invoke(method)
            if (method == failMethod) throw methodFailure
        } finally {
            activeCalls--
        }
    }

    override suspend fun fetchIdentity(): AccountIdentity {
        record("identity")
        return identityValue
    }

    override suspend fun fetchAccount(): AccountSnapshot {
        record("account")
        return snapshotValue
    }

    override suspend fun fetchTrips(): List<TripListItem> {
        record("trips")
        return tripValues
    }

    override suspend fun fetchTripDetail(id: String): TripDetailResponse {
        record("detail")
        return TripDetailResponse.encoding(detailValue)
    }

    override suspend fun send(change: OutboxChange): MutationReceipt {
        changes += change
        record("send")
        if (failures.isNotEmpty()) {
            val failure = failures.first()
            failures = failures.drop(1)
            if (failure !=
                null
            ) {
                throw failure
            }
        }
        if (receipts.isEmpty()) return MutationReceipt.Acknowledged
        return receipts.first().also { receipts = receipts.drop(1) }
    }

    override suspend fun passkeyOptions(): ApiPasskeyChallenge = throw DataFailure.Server(null)

    override suspend fun passkeyLogin(input: ApiPasskeyLoginInput): SessionResult = throw DataFailure.Server(null)

    override suspend fun signIn(input: PasswordSignInInput): SignInResult = throw DataFailure.Server(null)

    override suspend fun completeTwoFactor(input: TwoFactorInput): SessionResult = throw DataFailure.Server(null)

    override suspend fun signUp(input: RegisterInput): SessionResult = throw DataFailure.Server(null)

    override suspend fun requestPasswordReset(email: String) = throw DataFailure.Server(null)

    override suspend fun resetPassword(input: ResetPasswordInput) = throw DataFailure.Server(null)

    override suspend fun verificationStatus(): EmailVerificationStatus = throw DataFailure.Server(null)

    override suspend fun resendVerificationEmail(): EmailVerificationStatus = throw DataFailure.Server(null)

    override suspend fun verifyEmail(input: SignedVerificationInput): EmailVerificationStatus =
        throw DataFailure.Server(null)

    override suspend fun signOut() = throw DataFailure.Server(null)

    override suspend fun deleteAccount(password: String): Unit = throw DataFailure.Server(null)

    override suspend fun accountSecurity(change: AccountSecurityChange): AccountSecurityResult =
        throw DataFailure.Server(null)

    override suspend fun searchAirports(
        query: String,
        limit: Int?,
    ): List<ApiAirportSuggestion> = throw DataFailure.Server(null)

    override suspend fun reviewTrip(input: ApiTripReviewInput): ApiTripReview = throw DataFailure.Server(null)

    override suspend fun reviewGuestTrip(input: ApiGuestTripReviewInput): ApiTripReview = throw DataFailure.Server(null)

    override suspend fun reviewTripEdit(
        input: ApiTripReviewInput,
        id: String,
        sourceRevision: String,
    ): ApiTripReview = throw DataFailure.Server(null)

    override suspend fun saveTrip(identity: ApiTripSaveInput): ApiTripSaveResult = throw DataFailure.Server(null)

    override suspend fun updateTrip(
        id: String,
        identity: ApiTripSaveInput,
        sourceRevision: String,
    ): ApiTripSaveResult = throw DataFailure.Server(null)

    override suspend fun tripInput(id: String): ApiTripInputSnapshot = throw DataFailure.Server(null)
}
