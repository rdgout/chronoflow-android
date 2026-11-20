package net.chronoflow.core.wizard.model

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.wizard.generation.TripGenerationReviewResult
import kotlin.math.ceil
import kotlin.time.TimeSource

class WizardCheckingClock(
    val nowMillis: () -> Long,
    val sleepUntilMillis: suspend (Long) -> Unit,
) {
    companion object {
        val continuous: WizardCheckingClock by lazy {
            val origin = TimeSource.Monotonic.markNow()
            val now = { origin.elapsedNow().inWholeMilliseconds }
            WizardCheckingClock(now) { deadline -> delay((deadline - now()).coerceAtLeast(0)) }
        }
    }
}

data class WizardCheckingProgress(
    val ticks: Int = 0,
    val slow: Boolean = false,
    val failure: DataFailure? = null,
    val retryAt: Long? = null,
) {
    fun retrySeconds(atMillis: Long): Int = ceil(((retryAt ?: atMillis) - atMillis).coerceAtLeast(0) / 1000.0).toInt()
}

object WizardChecking {
    internal const val TICK_1_MS = 400L
    internal const val TICK_2_MS = 800L
    internal const val MINIMUM_MS = 1200L
    internal const val PAUSE_MS = 300L
    internal const val SLOW_MS = 10_000L
    internal const val TIMEOUT_MS = 30_000L

    sealed interface Event {
        data class Tick(
            val count: Int,
        ) : Event

        data object Slow : Event

        data class Complete(
            val result: TripGenerationReviewResult,
        ) : Event

        data class Failed(
            val failure: DataFailure,
        ) : Event
    }

    suspend fun run(
        clock: WizardCheckingClock,
        request: suspend () -> TripGenerationReviewResult,
        receive: (Event) -> Unit,
    ) {
        val start = clock.nowMillis()
        coroutineScope {
            val finished = CompletableDeferred<Unit>()
            launch {
                try {
                    val response = request()
                    clock.sleepUntilMillis(start + MINIMUM_MS)
                    currentCoroutineContext().ensureActive()
                    receive(Event.Tick(3))
                    clock.sleepUntilMillis(clock.nowMillis() + PAUSE_MS)
                    currentCoroutineContext().ensureActive()
                    receive(Event.Complete(response))
                } catch (failure: CancellationException) {
                    currentCoroutineContext().ensureActive()
                    receive(Event.Failed(DataFailure.Server(null)))
                } catch (failure: DataFailure) {
                    receive(Event.Failed(failure))
                } catch (_: Exception) {
                    receive(Event.Failed(DataFailure.Server(null)))
                }
            }.invokeOnCompletion { finished.complete(Unit) }
            launch {
                for ((time, event) in listOf(
                    TICK_1_MS to Event.Tick(1),
                    TICK_2_MS to Event.Tick(2),
                    SLOW_MS to Event.Slow,
                )) {
                    clock.sleepUntilMillis(start + time)
                    currentCoroutineContext().ensureActive()
                    receive(event)
                }
                clock.sleepUntilMillis(start + TIMEOUT_MS)
                currentCoroutineContext().ensureActive()
                receive(Event.Failed(DataFailure.Server(null)))
            }.invokeOnCompletion { finished.complete(Unit) }
            finished.await()
            coroutineContext.cancelChildren()
        }
    }
}
