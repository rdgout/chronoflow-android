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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.api.ApiTripReviewInput
import net.chronoflow.core.model.api.ApiTripSaveInput
import net.chronoflow.core.model.contracts.ApiTripReview
import net.chronoflow.core.model.contracts.ProfilePreferences
import net.chronoflow.core.wizard.model.WizardFingerprint
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToLong

class TripGenerationEffects(
    val mergeProfile: suspend (JsonObject) -> Unit,
    val pullCreatedTrip: suspend (String) -> Unit,
    val requestSync: suspend () -> Unit,
) {
    companion object
}

class TripGenerationService(
    private val source: DataSource,
    private val effects: TripGenerationEffects,
    private val sleep: suspend (Double) -> Unit = { delay((it * 1000).roundToLong()) },
    parentContext: CoroutineContext = Dispatchers.Default,
) : MemberWizardGeneration {
    private data class ReviewKey(
        val instance: String,
        val fingerprint: String,
        val token: String?,
        val reviewRevision: Int?,
        val edit: TripEditContext?,
    )

    private data class SaveKey(
        val instance: String,
        val identity: ApiTripSaveInput,
        val edit: TripEditContext?,
    )

    private class Pending<T>(
        val id: Any,
        val task: Deferred<T>,
    )

    private val job = SupervisorJob(parentContext[Job])
    private val scope = CoroutineScope(parentContext + job)
    private val mutex = Mutex()
    private val reviews = mutableMapOf<ReviewKey, Pending<TripGenerationReviewResult>>()
    private val saves = mutableMapOf<SaveKey, Pending<String>>()
    private val savedTrips = ConcurrentHashMap<SaveKey, String>()

    @Volatile private var cancelled = false

    override suspend fun review(
        instance: String,
        input: ApiTripReviewInput,
        edit: TripEditContext?,
    ): TripGenerationReviewResult {
        checkActive()
        val request = input.copy(profile = input.profile?.let(ProfilePreferences::request))
        val key =
            ReviewKey(
                instance,
                WizardFingerprint.canonical(request.copy(identity = null)),
                request.identity?.token,
                request.identity?.reviewRevision,
                edit,
            )
        val pending =
            mutex.withLock {
                checkActive()
                reviews[key] ?: run {
                    val id = Any()
                    val task =
                        scope.async(start = CoroutineStart.LAZY) {
                            try {
                                val result =
                                    coroutineScope {
                                        val deadline =
                                            launch {
                                                sleep(30.0)
                                                ensureActive()
                                                throw DataFailure.Server(null)
                                            }
                                        try {
                                            performReview(source, request, edit)
                                        } finally {
                                            deadline.cancel()
                                        }
                                    }
                                checkActive()
                                result.input.profile?.takeIf { it.isNotEmpty() }?.let {
                                    try {
                                        effects.mergeProfile(it)
                                    } catch (failure: CancellationException) {
                                        throw failure
                                    } catch (failure: Exception) {
                                        effects.requestSync()
                                        throw failure
                                    }
                                    effects.requestSync()
                                }
                                result
                            } finally {
                                withContext(NonCancellable) {
                                    mutex.withLock { if (reviews[key]?.id === id) reviews.remove(key) }
                                }
                            }
                        }
                    Pending(id, task).also { reviews[key] = it }
                }
            }
        pending.task.start()
        val result = pending.task.await()
        checkActive()
        return result
    }

    suspend fun answerPrompts(
        instance: String,
        input: ApiTripReviewInput,
        review: ApiTripReview,
        answers: Map<
            String,
            Map<
                String,
                JsonElement,
            >,
        >,
        edit: TripEditContext? = null,
    ): TripGenerationReviewResult =
        review(instance, TripGenerationAnswers.applyingAnswers(input, review, answers), edit)

    suspend fun save(initial: TripGenerationState): TripGenerationSaveResult = save(initial) {}

    override suspend fun save(
        state: TripGenerationState,
        didCreate: suspend (String) -> Unit,
    ): TripGenerationSaveResult {
        checkActive()
        var current = state
        try {
            val tripID = current.savedTripId ?: createTrip(current)
            current = TripGenerationState(current.instance, current.input, current.edit, current.review, tripID)
            didCreate(tripID)
            try {
                effects.pullCreatedTrip(tripID)
            } catch (failure: CancellationException) {
                throw failure
            } catch (_: Exception) {
                // A later sync can pull a trip whose server save already succeeded.
            }
            checkActive()
            effects.requestSync()
            return TripGenerationSaveResult(current, TripGenerationSaveResult.Outcome.Saved(tripID))
        } catch (failure: CancellationException) {
            if (!currentCoroutineContext().isActive || cancelled || !job.isActive || current.savedTripId == null) {
                throw failure
            }
            return TripGenerationSaveResult(current, TripGenerationSaveResult.Outcome.Failed(DataFailure.Server(null)))
        } catch (failure: DataFailure) {
            if (current.savedTripId == null &&
                (failure == DataFailure.ReviewExpired || failure == DataFailure.NotEnoughCredits)
            ) {
                val cleared = TripGenerationState(current.instance, current.input.copy(identity = null), current.edit)
                val reason =
                    if (failure == DataFailure.ReviewExpired) {
                        TripGenerationSaveResult.Reason.reviewExpired
                    } else {
                        TripGenerationSaveResult.Reason.notEnoughCredits
                    }
                return TripGenerationSaveResult(cleared, TripGenerationSaveResult.Outcome.ReReviewNeeded(reason))
            }
            return TripGenerationSaveResult(current, TripGenerationSaveResult.Outcome.Failed(failure))
        } catch (_: Exception) {
            return TripGenerationSaveResult(current, TripGenerationSaveResult.Outcome.Failed(DataFailure.Server(null)))
        }
    }

    private suspend fun createTrip(state: TripGenerationState): String {
        val review = state.review?.takeIf { it.canSave } ?: throw DataFailure.ReviewExpired
        val identity = ApiTripSaveInput.from(review.identity)
        val edit = state.edit
        val key = SaveKey(state.instance, identity, edit)
        val pending =
            mutex.withLock {
                checkActive()
                savedTrips[key]?.let { return it }
                saves[key] ?: run {
                    val id = Any()
                    val task =
                        scope.async(start = CoroutineStart.LAZY) {
                            try {
                                val result =
                                    if (edit == null) {
                                        source.saveTrip(identity)
                                    } else {
                                        source.updateTrip(
                                            edit.tripID,
                                            identity,
                                            edit.sourceRevision,
                                        )
                                    }
                                checkActive()
                                mutex.withLock {
                                    checkActive()
                                    savedTrips[key] = result.tripId
                                }
                                result.tripId
                            } finally {
                                withContext(NonCancellable) {
                                    mutex.withLock { if (saves[key]?.id === id) saves.remove(key) }
                                }
                            }
                        }
                    Pending(id, task).also { saves[key] = it }
                }
            }
        pending.task.start()
        return pending.task.await().also { checkActive() }
    }

    override suspend fun cancelReviews(instance: String) {
        mutex.withLock {
            val matching = reviews.filterKeys { it.instance == instance }
            matching.forEach { (key, pending) ->
                reviews.remove(key)
                pending.task.cancel()
            }
        }
    }

    fun cancelNow() {
        cancelled = true
        job.cancel()
        savedTrips.clear()
    }

    override suspend fun cancel() = cancelNow()

    private suspend fun checkActive() {
        currentCoroutineContext().ensureActive()
        job.ensureActive()
        if (cancelled) throw CancellationException()
    }
}
