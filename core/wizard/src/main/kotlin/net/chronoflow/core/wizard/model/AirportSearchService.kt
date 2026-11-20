package net.chronoflow.core.wizard.model

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.contracts.ApiAirportSuggestion
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToLong

class AirportSearchService(
    private val search: suspend (String) -> List<ApiAirportSuggestion>,
    private val now: () -> Instant,
    private val sleep: suspend (Double) -> Unit,
    private val cache: AirportSearchCache? = null,
) {
    constructor(
        source: DataSource,
        now: () -> Instant,
        sleep: suspend (Double) -> Unit,
        cache: AirportSearchCache = AirportSearchCache(),
    ) : this({ source.searchAirports(it, 8) }, now, sleep, cache)

    private val mutex = Mutex()
    private var nextRequest = Instant.MIN

    suspend fun results(raw: String): List<ApiAirportSuggestion> {
        val query = raw.trim()
        if (query.codePointCount(0, query.length) !in 2..60) return emptyList()
        sleep(0.35)
        currentCoroutineContext().ensureActive()
        cache?.get(query, now())?.let { return it }
        while (true) {
            val wait =
                mutex.withLock {
                    val current = now()
                    if (current >= nextRequest) {
                        nextRequest = current.plusMillis(1020)
                        null
                    } else {
                        Duration.between(current, nextRequest).let { it.seconds + it.nano / 1_000_000_000.0 }
                    }
                } ?: break
            sleep(wait)
            currentCoroutineContext().ensureActive()
        }
        currentCoroutineContext().ensureActive()
        try {
            return search(query).also { cache?.put(query, it, now()) }
        } catch (failure: DataFailure.Throttled) {
            val seconds = failure.retryAfterSeconds
            if (seconds.isFinite() && seconds > 0) {
                mutex.withLock {
                    nextRequest = maxOf(nextRequest, now().plusMillis((seconds * 1000).roundToLong()))
                }
            }
            throw failure
        }
    }
}
