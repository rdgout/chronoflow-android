@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.SerializationException
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.model.contracts.TripStatus
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.plan.TripListValue
import net.chronoflow.core.plan.tripDisplayStatus
import net.chronoflow.core.plan.tripSections
import net.chronoflow.core.sync.refresh.RefreshFeedbackModel
import net.chronoflow.core.ui.trip.tripCardUi
import java.time.Instant

class TripsViewModel(
    private val deps: TripsDependencies,
) : ViewModel() {
    private val language = MutableStateFlow<Language?>(null)
    private val feedback = RefreshFeedbackModel(viewModelScope, deps.refreshProbe)
    val refreshState = feedback.state
    val refreshProbe = deps.refreshProbe
    private val ticks = deps.clock.values().shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)
    val now = ticks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Instant.EPOCH)
    private val _formatter = MutableStateFlow<CopyLineFormatter?>(null)
    val formatter: kotlinx.coroutines.flow.StateFlow<CopyLineFormatter?> = _formatter
    val state =
        states()
            .flowOn(deps.worker)
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TripsUiState.Loading)

    fun setLanguage(language: Language) {
        this.language.value = language
    }

    fun refresh() {
        feedback.refresh { deps.refreshNow() }
    }

    fun dismissFeedback(id: Long) = feedback.dismiss(id)

    private data class Inputs(
        val now: Instant,
        val synced: Boolean,
        val preferences: TripsPreferences,
        val language: Language?,
    )

    private fun states(): Flow<TripsUiState> =
        flow {
            val formatter = deps.formatter().also { _formatter.value = it }
            coroutineScope {
                val now = ticks.stateIn(this)
                val decoded =
                    deps
                        .tripRecords()
                        .map { deps.decoder.trips(it, now.value).values }
                        .shareIn(this, SharingStarted.WhileSubscribed(), replay = 1)
                val values =
                    combine(decoded, now) { trips, instant ->
                        trips.map { value ->
                            val status = tripDisplayStatus(value.card, value.planStartUtc, value.planEndUtc, instant)
                            value.copy(status = status)
                        }
                    }.shareIn(this, SharingStarted.WhileSubscribed(), replay = 1)
                val activeIds =
                    values
                        .map { trips ->
                            trips.filter { it.status == TripStatus.Active }.map { it.card.id }.distinct()
                        }.distinctUntilChanged()
                var previousDetails = emptyMap<String, TripDetail?>()
                val details =
                    activeIds.flatMapLatest { ids ->
                        previousDetails = previousDetails.filterKeys { it in ids }
                        if (ids.isEmpty()) {
                            flowOf(emptyMap())
                        } else {
                            val observations =
                                ids.map { id ->
                                    deps.planSnapshots(id).map { snapshot ->
                                        val detail =
                                            try {
                                                deps.decoder.detail(snapshot)
                                            } catch (_: SerializationException) {
                                                null
                                            } catch (_: IllegalArgumentException) {
                                                null
                                            }
                                        id to detail
                                    }
                                }
                            combine(observations) { it.toMap() }
                                .onStart { emit(previousDetails) }
                                .onEach { previousDetails = it }
                        }
                    }
                val inputs = combine(now, deps.synced(), deps.preferences(), language, ::Inputs)
                emitAll(
                    combine(values, details, inputs) { trips, plans, input ->
                        val currentLanguage = input.language
                        if (!input.synced || currentLanguage == null) {
                            TripsUiState.Loading
                        } else {
                            val sections =
                                tripSections(trips).map { section ->
                                    TripSectionUi(
                                        section.status,
                                        section.trips.map { value ->
                                            tripCardUi(
                                                value,
                                                plans[value.card.id],
                                                input.now,
                                                net.chronoflow.core.plan.trippage.PlanPreferences(
                                                    input.preferences.timeFormat,
                                                    input.preferences.hidden,
                                                ),
                                                currentLanguage,
                                                formatter,
                                            )
                                        },
                                    )
                                }
                            if (sections.isEmpty()) TripsUiState.Empty else TripsUiState.Content(sections)
                        }
                    },
                )
            }
        }

    companion object {
        val Factory = viewModelFactory { initializer { TripsViewModel(checkNotNull(this[TripsDependenciesKey])) } }
    }
}
