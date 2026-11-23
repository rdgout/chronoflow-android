package net.chronoflow.core.testing.wizard

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
import net.chronoflow.core.model.contracts.ApiReviewIdentity
import net.chronoflow.core.model.contracts.ApiTripInputSnapshot
import net.chronoflow.core.model.contracts.ApiTripReview
import net.chronoflow.core.model.contracts.ApiTripSaveResult
import net.chronoflow.core.model.contracts.TripDetailResponse
import net.chronoflow.core.model.contracts.TripListItem
import net.chronoflow.core.wizard.generation.TripEditContext
import java.util.concurrent.ConcurrentHashMap

class GenerationSource(
    val mock: DataSource? = null,
) : DataSource {
    private val mutex = Mutex()
    private val counts = ConcurrentHashMap<String, Int>()
    private val inputs = mutableListOf<ApiTripReviewInput>()
    private val guests = mutableListOf<ApiGuestTripReviewInput>()
    private val edits = mutableListOf<TripEditContext>()
    private val ops = mutableListOf<String>()
    private val waiters = mutableListOf<Triple<String, Int, CompletableDeferred<Unit>>>()
    private var reviewResults = ArrayDeque<Result<ApiTripReview>>()
    private var snapshots = ArrayDeque<ApiTripInputSnapshot>()

    @Volatile var reviewFailure: DataFailure? = null
        private set

    @Volatile var saveFailure: DataFailure? = null
        private set

    @Volatile var beforeReview: suspend () -> Unit = {}
        private set

    @Volatile var beforeSave: suspend () -> Unit = {}
        private set

    @Volatile var lastEdit: TripEditContext? = null
        private set

    @Volatile var guestHandler: (suspend (ApiGuestTripReviewInput) -> ApiTripReview)? = null
    private var loseNextSaveResponse = false
    val reviewInputs: List<ApiTripReviewInput> get() = synchronized(inputs) { inputs.toList() }
    val guestInputs: List<ApiGuestTripReviewInput> get() = synchronized(guests) { guests.toList() }
    val editRequests: List<TripEditContext> get() = synchronized(edits) { edits.toList() }
    val operations: List<String> get() = synchronized(ops) { ops.toList() }

    fun configure(
        reviewFailure: DataFailure? = null,
        saveFailure: DataFailure? = null,
        beforeReview: suspend () -> Unit = {
        },
        beforeSave: suspend () -> Unit = {},
    ) {
        this.reviewFailure = reviewFailure
        this.saveFailure = saveFailure
        this.beforeReview = beforeReview
        this.beforeSave = beforeSave
    }

    suspend fun count(method: String): Int = counts[method] ?: 0

    suspend fun queueReviews(results: List<Result<ApiTripReview>>) {
        mutex.withLock {
            reviewResults =
                ArrayDeque(results)
        }
    }

    suspend fun queueSnapshots(values: List<ApiTripInputSnapshot>) {
        mutex.withLock { snapshots = ArrayDeque(values) }
    }

    suspend fun loseSaveResponseOnce() {
        mutex.withLock { loseNextSaveResponse = true }
    }

    private suspend fun record(method: String) {
        mutex.withLock {
            synchronized(ops) { ops.add(method) }
            counts[method] = (counts[method] ?: 0) + 1
            val ready = waiters.filter { it.first == method && it.second <= counts.getValue(method) }
            waiters.removeAll(ready.toSet())
            ready.forEach { it.third.complete(Unit) }
        }
    }

    suspend fun waitForReviews(count: Int) = waitFor("review", count)

    suspend fun waitForSaves(count: Int) = waitFor("save", count)

    private suspend fun waitFor(
        method: String,
        count: Int,
    ) {
        val waiter =
            mutex.withLock {
                if ((counts[method] ?: 0) >=
                    count
                ) {
                    null
                } else {
                    CompletableDeferred<Unit>().also { waiters.add(Triple(method, count, it)) }
                }
            }
        waiter?.await()
    }

    override suspend fun reviewTrip(input: ApiTripReviewInput): ApiTripReview {
        synchronized(inputs) { inputs.add(input) }
        record("review")
        beforeReview()
        mutex.withLock { reviewResults.removeFirstOrNull() }?.let { return it.getOrThrow() }
        reviewFailure?.let { throw it }
        return mock?.reviewTrip(input) ?: generationReview()
    }

    override suspend fun reviewTripEdit(
        input: ApiTripReviewInput,
        id: String,
        sourceRevision: String,
    ): ApiTripReview {
        val edit = TripEditContext(id, sourceRevision)
        lastEdit = edit
        synchronized(edits) { edits.add(edit) }
        val review = reviewTrip(input)
        return if (review.identity.sourceRevision ==
            null
        ) {
            review.copy(identity = review.identity.copy(sourceRevision = sourceRevision))
        } else {
            review
        }
    }

    override suspend fun saveTrip(identity: ApiTripSaveInput): ApiTripSaveResult {
        record("save")
        beforeSave()
        saveFailure?.let { throw it }
        val result = mock?.saveTrip(identity) ?: ApiTripSaveResult("created")
        val lose = mutex.withLock { loseNextSaveResponse.also { loseNextSaveResponse = false } }
        if (lose) throw DataFailure.Offline
        return result
    }

    override suspend fun updateTrip(
        id: String,
        identity: ApiTripSaveInput,
        sourceRevision: String,
    ): ApiTripSaveResult {
        record("update")
        beforeSave()
        saveFailure?.let { throw it }
        lastEdit = TripEditContext(id, sourceRevision)
        return ApiTripSaveResult(id)
    }

    override suspend fun fetchTrips(): List<TripListItem> {
        record("trips")
        return (
            mock
                ?: throw DataFailure.Server(null)
        ).fetchTrips()
    }

    override suspend fun fetchTripDetail(id: String): TripDetailResponse {
        record("detail")
        return (
            mock
                ?: throw DataFailure.Server(null)
        ).fetchTripDetail(id)
    }

    override suspend fun tripInput(id: String): ApiTripInputSnapshot {
        record("input")
        return mutex.withLock { snapshots.removeFirstOrNull() }
            ?: ApiTripInputSnapshot(generationInput(), "revision", emptyMap(), null)
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

    override suspend fun fetchIdentity(): AccountIdentity = throw DataFailure.Server(null)

    override suspend fun fetchAccount(): AccountSnapshot = throw DataFailure.Server(null)

    override suspend fun searchAirports(
        query: String,
        limit: Int?,
    ): List<ApiAirportSuggestion> = throw DataFailure.Server(null)

    override suspend fun reviewGuestTrip(input: ApiGuestTripReviewInput): ApiTripReview {
        synchronized(guests) { guests.add(input) }
        return guestHandler?.invoke(input) ?: throw DataFailure.Server(null)
    }

    override suspend fun send(change: OutboxChange): MutationReceipt = throw DataFailure.Server(null)
}

fun expiredIdentity(): ApiReviewIdentity = ApiReviewIdentity("expired", 1, 1, null, null)
