package net.chronoflow.core.wizard.generation

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.api.ApiGuestTripReviewInput
import net.chronoflow.core.model.api.ApiTripReviewInput
import net.chronoflow.core.model.api.TripWizardErrors
import net.chronoflow.core.model.contracts.ApiTripInputOverride
import net.chronoflow.core.model.contracts.ApiTripReview
import net.chronoflow.core.model.contracts.ProfilePreferences
import net.chronoflow.core.model.guest.GuestSecretStore
import net.chronoflow.core.wizard.model.WizardFingerprint
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToLong

class GuestWizardGeneration(
    private val source: DataSource,
    private val secrets: GuestSecretStore,
    private val deviceIdentity: suspend () -> String,
    private val sleep: suspend (Double) -> Unit = { delay((it * 1000).roundToLong()) },
    parentContext: CoroutineContext = Dispatchers.Default,
) : MemberWizardGeneration {
    private data class ReviewKey(
        val instance: String,
        val fingerprint: String,
    )

    private class Flight(
        val id: Any,
        val task: Deferred<TripGenerationReviewResult>,
        val result: AtomicReference<TripGenerationReviewResult?>,
    )

    private val job = SupervisorJob(parentContext[Job])
    private val scope = CoroutineScope(parentContext + job)
    private val mutex = Mutex()
    private val reviews = ConcurrentHashMap<ReviewKey, Flight>()
    private val current = ConcurrentHashMap<String, ReviewKey>()

    @Volatile private var cancelled = false

    override suspend fun review(
        instance: String,
        input: ApiTripReviewInput,
        edit: TripEditContext?,
    ): TripGenerationReviewResult {
        if (edit != null) throw DataFailure.Server(null)
        checkActive()
        val request =
            input.copy(
                profile = ProfilePreferences.request(input.profile ?: JsonObject(emptyMap())),
                identity = input.identity?.copy(guestSecret = null),
                input =
                    input.input.copy(
                        `return` = null,
                        tripEndKind = "one_way",
                        tripEndNights = null,
                        tripEndCheckOutDate = null,
                        override = ApiTripInputOverride(false, null, null, null),
                    ),
            )
        val key = ReviewKey(instance, WizardFingerprint.canonical(request))
        val flight =
            mutex.withLock {
                checkActive()
                reviews[key] ?: run {
                    current[instance] = key
                    val id = Any()
                    val result = AtomicReference<TripGenerationReviewResult?>()
                    val task =
                        scope.async(start = CoroutineStart.LAZY) {
                            try {
                                coroutineScope {
                                    val deadline =
                                        launch {
                                            sleep(30.0)
                                            ensureActive()
                                            throw DataFailure.Server(null)
                                        }
                                    try {
                                        performReview(key, id, request).also { result.set(it) }
                                    } finally {
                                        deadline.cancel()
                                    }
                                }
                            } finally {
                                if (!currentCoroutineContext().isActive || !isCurrent(key, id)) {
                                    withContext(
                                        NonCancellable,
                                    ) { result.get()?.let { secrets.delete(it.review.identity.token) } }
                                }
                            }
                        }
                    Flight(id, task, result).also { reviews[key] = it }
                }
            }
        flight.task.start()
        try {
            val result = flight.task.await()
            if (!currentCoroutineContext().isActive || cancelled || current[instance] != key) {
                withContext(NonCancellable) { secrets.delete(result.review.identity.token) }
                throw CancellationException()
            }
            return result
        } catch (failure: CancellationException) {
            if (!currentCoroutineContext().isActive) {
                withContext(NonCancellable) {
                    mutex.withLock {
                        if (reviews[key]?.id === flight.id) reviews.remove(key)?.task?.cancel()
                    }
                    flight.result.get()?.let { secrets.delete(it.review.identity.token) }
                }
            }
            throw failure
        } finally {
            withContext(NonCancellable) {
                mutex.withLock { if (reviews[key]?.id === flight.id) reviews.remove(key) }
            }
        }
    }

    private suspend fun performReview(
        key: ReviewKey,
        id: Any,
        input: ApiTripReviewInput,
    ): TripGenerationReviewResult {
        val oldToken = input.identity?.token
        val secret = oldToken?.let { secrets.get(it) }
        val deviceID = deviceIdentity()
        currentCoroutineContext().ensureActive()
        val request =
            ApiGuestTripReviewInput(
                input.input,
                input.identity,
                input.profile ?: JsonObject(emptyMap()),
                deviceID,
                secret,
            )
        val review =
            try {
                send(request)
            } catch (failure: DataFailure.ReviewExpired) {
                if (oldToken == null || !isCurrent(key, id) || !currentCoroutineContext().isActive) throw failure
                send(request.copy(identity = null))
            }
        val token = review.identity.token
        try {
            val next = review.identity.guestSecret ?: secret
            if (next != null) secrets.rotate(oldToken, token, next)
            if (!currentCoroutineContext().isActive || !isCurrent(key, id)) throw CancellationException()
            val identity = review.identity.copy(guestSecret = null)
            return TripGenerationReviewResult(input.copy(identity = identity), review.copy(identity = identity))
        } finally {
            if (!currentCoroutineContext().isActive || !isCurrent(key, id)) {
                withContext(NonCancellable) { secrets.delete(token) }
            }
        }
    }

    private suspend fun send(input: ApiGuestTripReviewInput): ApiTripReview =
        try {
            source.reviewGuestTrip(input)
        } catch (failure: DataFailure) {
            throw TripWizardErrors.map(failure, input.input.outbound.legs.size)
        }

    private fun isCurrent(
        key: ReviewKey,
        id: Any,
    ): Boolean = !cancelled && current[key.instance] == key && reviews[key]?.id === id

    private suspend fun checkActive() {
        currentCoroutineContext().ensureActive()
        job.ensureActive()
        if (cancelled) throw CancellationException()
    }

    override suspend fun save(
        state: TripGenerationState,
        didCreate: suspend (String) -> Unit,
    ): TripGenerationSaveResult = throw DataFailure.Server(null)

    override suspend fun cancelReviews(instance: String) {
        mutex.withLock {
            reviews.entries.filter { it.key.instance == instance }.forEach { (key, flight) ->
                reviews.remove(key)
                flight.task.cancel()
            }
            current.remove(instance)
        }
    }

    override fun cancelNow() {
        cancelled = true
        job.cancel()
        reviews.clear()
        current.clear()
    }

    override suspend fun cancel() = cancelNow()
}
