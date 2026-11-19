@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.model.device.DeviceIdentityFailure
import net.chronoflow.core.model.session.SessionFailure
import net.chronoflow.core.model.store.LocalMutationFailure
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.plan.PlanClock
import net.chronoflow.core.plan.PlanDecoder
import net.chronoflow.core.plan.selectTodayTrip
import net.chronoflow.core.plan.step.StepSheetState
import net.chronoflow.core.plan.step.stepSheetState
import net.chronoflow.core.plan.tripDisplayStatus
import net.chronoflow.core.plan.trippage.PlanPreferences
import net.chronoflow.core.sync.SyncResult
import net.chronoflow.core.sync.refresh.RefreshDebugProbe
import net.chronoflow.core.sync.refresh.RefreshFeedbackModel
import net.chronoflow.feature.today.data.TodaySource
import java.io.IOException
import java.security.GeneralSecurityException
import java.time.Instant

class TodayViewModel(
    private val source: TodaySource,
    clock: PlanClock,
    formatter: suspend () -> CopyLineFormatter,
    worker: CoroutineDispatcher,
    main: CoroutineDispatcher,
    val refreshProbe: RefreshDebugProbe = RefreshDebugProbe.None,
) : ViewModel(CoroutineScope(SupervisorJob() + main)) {
    private val feedback = RefreshFeedbackModel(viewModelScope, refreshProbe)
    val refreshState = feedback.state

    suspend fun refresh(action: suspend () -> SyncResult?) {
        feedback.refresh(action = action)?.join()
    }

    fun dismissFeedback(id: Long) = feedback.dismiss(id)

    private val sharing = SharingStarted.WhileSubscribed(5_000)
    private val ticks = clock.values().shareIn(viewModelScope, sharing, replay = 1)
    val now = ticks.stateIn(viewModelScope, sharing, Instant.EPOCH)
    private val language = MutableStateFlow<Language>(Language.En)
    val formatter =
        flow {
            val lines =
                try {
                    formatter()
                } catch (_: IOException) {
                    null
                } catch (_: SerializationException) {
                    null
                } catch (_: IllegalArgumentException) {
                    null
                }
            emit(lines)
        }.flowOn(worker).stateIn(viewModelScope, sharing, null)
    val preferences = source.preferences().stateIn(viewModelScope, sharing, PlanPreferences())
    private val decoder = PlanDecoder(worker)
    private val decoded = source.trips().distinctUntilChanged().map { decoder.trips(it, ticks.first()).values }

    private data class TripTick(
        val values: List<net.chronoflow.core.plan.TripListValue>,
        val now: Instant,
    )

    private val trips =
        combine(decoded, ticks) { values, now ->
            val updated =
                values.map { value ->
                    val status = tripDisplayStatus(value.card, value.planStartUtc, value.planEndUtc, now)
                    value.copy(status = status)
                }
            TripTick(updated, now)
        }.flowOn(worker).stateIn(viewModelScope, sharing, null)
    private val heroId =
        trips
            .map { tick -> tick?.let { selectTodayTrip(it.values, it.now)?.card?.id } }
            .distinctUntilChanged()
            .flowOn(worker)
            .stateIn(viewModelScope, sharing, null)
    private val _stepSheet = MutableStateFlow<StepSelection?>(null)
    val stepSheet = _stepSheet.asStateFlow()
    private val details = details().flowOn(worker).stateIn(viewModelScope, sharing, emptyMap())
    private val heroDetail = combine(heroId, details) { id, values -> values[id] }
    private val selectionDetail = combine(stepSheet, details) { selection, values -> values[selection?.tripId] }
    private val inputs =
        combine(trips, source.synced(), preferences, heroDetail) { tick, synced, prefs, detail ->
            TodayInput(tick?.values, synced, prefs, detail, tick?.now ?: Instant.EPOCH, Language.En, null)
        }
    val uiState =
        combine(inputs, language, this.formatter) { input, language, lines ->
            TodayReducer.reduce(input.copy(language = language, formatter = lines))
        }.flowOn(worker).distinctUntilChanged().stateIn(viewModelScope, sharing, TodayUiState.Loading)

    private data class SheetInput(
        val selection: StepSelection?,
        val detail: TripDetail?,
        val now: Instant,
        val preferences: PlanPreferences,
        val language: Language,
    )

    private val sheetInput = combine(stepSheet, selectionDetail, ticks, preferences, language, ::SheetInput)
    val stepSheetState =
        combine(sheetInput, this.formatter) { input, lines ->
            val selection = input.selection
            if (selection == null || lines == null) {
                null
            } else {
                stepSheetState(
                    input.detail?.takeIf { it.trip.id == selection.tripId },
                    selection.blockId,
                    input.now.toEpochMilli(),
                    lines,
                    input.language,
                    input.preferences.timeFormat,
                )
            }
        }.flowOn(worker).distinctUntilChanged().stateIn(viewModelScope, sharing, null)
    private val _changingCheck = MutableStateFlow(false)
    val changingCheck = _changingCheck.asStateFlow()
    private var mutationError: Throwable? = null

    private fun details(): Flow<Map<String, TripDetail?>> {
        var previous = emptyMap<String, TripDetail?>()
        return combine(heroId, stepSheet) { hero, selection -> listOfNotNull(hero, selection?.tripId).distinct() }
            .distinctUntilChanged()
            .flatMapLatest { ids ->
                previous = previous.filterKeys { it in ids }
                if (ids.isEmpty()) {
                    flowOf(emptyMap())
                } else {
                    val observations = ids.map { id -> source.detail(id).map { id to it } }
                    combine(observations) { it.toMap() }.onStart { emit(previous) }.onEach { previous = it }
                }
            }
    }

    fun setLanguage(language: Language) {
        this.language.value = language
    }

    fun openStep(blockId: String) {
        val id =
            when (val state = uiState.value) {
                is TodayUiState.Active -> state.hero.id
                is TodayUiState.Future -> state.hero.card.id
                else -> return
            }
        _stepSheet.value = StepSelection(id, blockId)
    }

    fun dismissStep() {
        _stepSheet.value = null
    }

    fun setCheck(
        blockId: String,
        status: CheckStatus?,
    ) {
        if (_changingCheck.value) return
        val selection = stepSheet.value ?: return
        if (blockId != selection.blockId) return
        val detail = details.value[selection.tripId]?.takeIf { it.trip.id == selection.tripId } ?: return
        _changingCheck.value = true
        viewModelScope.launch {
            try {
                source.setCheck(selection.tripId, detail, blockId, status)
                mutationError = null
            } catch (error: CancellationException) {
                if (!currentCoroutineContext().isActive) throw error
                mutationError = error
            } catch (error: SessionFailure) {
                mutationError = error
            } catch (error: DataFailure) {
                mutationError = error
            } catch (error: DeviceIdentityFailure) {
                mutationError = error
            } catch (error: StoreFailure) {
                mutationError = error
            } catch (error: LocalMutationFailure) {
                mutationError = error
            } catch (error: IOException) {
                mutationError = error
            } catch (error: GeneralSecurityException) {
                mutationError = error
            } catch (error: RuntimeException) {
                mutationError = error
            } finally {
                _changingCheck.value = false
            }
        }
    }

    companion object {
        val Factory =
            viewModelFactory {
                initializer {
                    val deps = checkNotNull(this[TodayDependenciesKey])
                    TodayViewModel(deps.source, deps.clock, deps.formatter, deps.worker, deps.main, deps.refreshProbe)
                }
            }
    }
}

data class TodayDependencies(
    val source: TodaySource,
    val clock: PlanClock,
    val formatter: suspend () -> CopyLineFormatter,
    val worker: CoroutineDispatcher,
    val main: CoroutineDispatcher,
    val refreshProbe: RefreshDebugProbe = RefreshDebugProbe.None,
)

object TodayDependenciesKey : CreationExtras.Key<TodayDependencies>
