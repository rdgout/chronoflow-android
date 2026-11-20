package net.chronoflow.core.wizard.model

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.contracts.ApiAirportSuggestion
import java.time.Instant
import kotlin.math.roundToLong

@OptIn(ExperimentalCoroutinesApi::class)
class AirportSearchSession(
    service: AirportSearchService,
    scope: CoroutineScope,
    private val now: () -> Instant,
) {
    private val query = MutableStateFlow("")
    private val retryCount = MutableStateFlow(0L)
    private var lastResults = emptyList<ApiAirportSuggestion>()
    val state: StateFlow<AirportSearchState> =
        combine(query, retryCount) { value, retry -> value to retry }
            .distinctUntilChanged()
            .transformLatest { (raw, _) ->
                val value = raw.trim()
                when (value.codePointCount(0, value.length)) {
                    0, 1 -> {
                        emit(AirportSearchState.Hint)
                    }

                    in 2..60 -> {
                        emit(AirportSearchState.Searching(value, lastResults))
                        try {
                            val results = service.results(raw)
                            lastResults = results
                            emit(
                                if (results.isEmpty()) {
                                    AirportSearchState.Empty(
                                        value,
                                    )
                                } else {
                                    AirportSearchState.Results(value, results)
                                },
                            )
                        } catch (failure: CancellationException) {
                            throw failure
                        } catch (failure: DataFailure.Throttled) {
                            val seconds = failure.retryAfterSeconds.takeIf { it.isFinite() && it > 0 } ?: 0.0
                            emit(AirportSearchState.Throttled(value, now().plusMillis((seconds * 1000).roundToLong())))
                        } catch (_: Exception) {
                            emit(AirportSearchState.Failed(value))
                        }
                    }

                    else -> {
                        emit(AirportSearchState.Empty(value))
                    }
                }
            }.stateIn(scope, SharingStarted.Eagerly, AirportSearchState.Hint)

    fun setQuery(raw: String) {
        query.value = raw
    }

    fun retry() {
        val current = state.value
        if (current is AirportSearchState.Throttled && now() < current.retryAt) return
        retryCount.update { it + 1 }
    }
}
