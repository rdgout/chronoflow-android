@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.feature.wizard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.contracts.ApiAirportSuggestion
import net.chronoflow.core.model.device.DeviceIdentityFailure
import net.chronoflow.core.model.session.SessionFailure
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.core.wizard.generation.TripGenerationService
import net.chronoflow.core.wizard.model.AirportSearchService
import net.chronoflow.core.wizard.model.AirportSearchSession
import net.chronoflow.core.wizard.model.AirportSearchState
import net.chronoflow.core.wizard.model.MemberWizardModel
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.RecoveryRejected
import net.chronoflow.core.wizard.model.TripWizardAction
import net.chronoflow.core.wizard.model.WizardAirportSheet
import net.chronoflow.core.wizard.model.finishRoute
import net.chronoflow.core.wizard.model.hasChanges
import net.chronoflow.core.wizard.model.navigationPath
import net.chronoflow.core.wizard.model.popNavigation
import net.chronoflow.core.wizard.model.progress
import net.chronoflow.core.wizard.model.resumeChecking
import net.chronoflow.core.wizard.model.resumeCompletion
import net.chronoflow.core.wizard.model.showsBack
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardExit
import net.chronoflow.feature.wizard.shell.WizardStepKey
import net.chronoflow.feature.wizard.ui.AirportRowUi
import net.chronoflow.feature.wizard.ui.AirportSearchUi
import net.chronoflow.feature.wizard.ui.WizardUiState
import java.io.IOException
import java.security.GeneralSecurityException
import java.time.Duration
import java.time.Instant
import net.chronoflow.core.navigation.WizardMode as WizardEntry

class TripWizardViewModel(
    entry: WizardEntry,
    deps: TripWizardDependencies?,
) : ViewModel() {
    private val mutableClosing = MutableStateFlow(false)
    val closing = mutableClosing.asStateFlow()
    private val mutableDialog = MutableStateFlow(false)
    val discardDialogVisible = mutableDialog.asStateFlow()
    private val mutableExit = MutableStateFlow<WizardExit?>(null)
    val exit = mutableExit.asStateFlow()
    private var selectedAirport = false
    private var continuing = false
    private val mutableState = MutableStateFlow<TripWizardUiState>(TripWizardUiState.Loading)
    val state: StateFlow<TripWizardUiState> = mutableState.asStateFlow()
    val uiState =
        combine(state, closing, discardDialogVisible) { value, closing, dialog ->
            (value as? TripWizardUiState.Active)?.let {
                WizardUiState.from(it, closing, dialog).let { ui ->
                    ui.copy(
                        chrome =
                            ui.chrome.copy(
                                showBack =
                                    wizard?.showsBack == true,
                            ),
                    )
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val chrome = uiState.map { it?.chrome }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val where = uiState.map { it?.where }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val sheet = uiState.map { it?.sheet }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private var generation: TripGenerationService? = null
    private var discarded = false
    var wizard: MemberWizardModel? = null
        private set
    val search =
        AirportSearchSession(
            deps?.airportSearch ?: AirportSearchService(
                {
                    throw DataFailure.Unauthorized(
                        401,
                    )
                },
                { Instant.EPOCH },
                {},
            ),
            viewModelScope,
            deps?.now ?: { Instant.EPOCH },
        )

    val searchState =
        combine(search.state, state) { result, current ->
            val sheet =
                (current as? TripWizardUiState.Active)
                    ?.wizard
                    ?.draft
                    ?.state
                    ?.sheet
            when (result) {
                AirportSearchState.Hint -> {
                    AirportSearchUi.Hint
                }

                is AirportSearchState.Searching -> {
                    AirportSearchUi.Searching
                }

                is AirportSearchState.Results -> {
                    val allowed =
                        if (sheet ==
                            null
                        ) {
                            emptyList()
                        } else {
                            result.airports.filter { wizard?.airportIsAllowed(it.iata, sheet) == true }
                        }
                    if (allowed.isEmpty()) {
                        AirportSearchUi.Empty
                    } else {
                        AirportSearchUi.Results(
                            allowed.map(AirportRowUi::from),
                        )
                    }
                }

                is AirportSearchState.Empty -> {
                    AirportSearchUi.Empty
                }

                is AirportSearchState.Failed -> {
                    AirportSearchUi.Failed(null)
                }

                is AirportSearchState.Throttled -> {
                    AirportSearchUi.Failed(result.retryAt)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AirportSearchUi.Hint)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val retryEnabled =
        searchState
            .transformLatest { value ->
                val retryAt = (value as? AirportSearchUi.Failed)?.retryAt
                val now = deps?.now?.invoke() ?: Instant.EPOCH
                val wait = retryAt?.let { Duration.between(now, it).toMillis().coerceAtLeast(0) } ?: 0L
                emit(wait == 0L)
                if (wait > 0L) {
                    delay(wait)
                    emit(true)
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    init {
        if (deps == null) {
            mutableState.value = TripWizardUiState.LoadFailed(DataFailure.Unauthorized(401))
            exitOnce()
        } else {
            viewModelScope.launch {
                try {
                    val draft =
                        when (entry) {
                            WizardEntry.Create -> deps.start()
                            is WizardEntry.Edit -> deps.startTrip(entry.tripId, TripWizardAction.edit)
                            is WizardEntry.Duplicate -> deps.startTrip(entry.tripId, TripWizardAction.duplicate)
                        }
                    currentCoroutineContext().ensureActive()
                    val service = deps.newGeneration().also { generation = it }
                    val model =
                        MemberWizardModel(
                            draft,
                            deps.store,
                            viewModelScope,
                            deps.now,
                            service,
                            deps.checkingClock,
                            writerScope = deps.writerScope,
                        )
                    wizard = model
                    model.resumeChecking()
                    model.resumeCompletion()
                    model.snapshots.collect { snapshot ->
                        val finished = snapshot.closed && snapshot.createdTripID != null
                        mutableState.value =
                            if (discarded || finished) {
                                TripWizardUiState.Closed(snapshot.createdTripID).also { exitOnce() }
                            } else {
                                TripWizardUiState.Active(snapshot, model.progress, model.navigationPath)
                            }
                    }
                } catch (_: CancellationException) {
                    currentCoroutineContext().ensureActive()
                    loadFailed(DataFailure.Server(null))
                } catch (failure: DataFailure) {
                    loadFailed(failure)
                } catch (_: SessionFailure) {
                    loadFailed(DataFailure.Server(null))
                } catch (_: DeviceIdentityFailure) {
                    loadFailed(DataFailure.Server(null))
                } catch (_: StoreFailure) {
                    loadFailed(DataFailure.Server(null))
                } catch (_: RecoveryRejected) {
                    loadFailed(DataFailure.Server(null))
                } catch (_: IOException) {
                    loadFailed(DataFailure.Server(null))
                } catch (_: GeneralSecurityException) {
                    loadFailed(DataFailure.Server(null))
                } catch (_: RuntimeException) {
                    loadFailed(DataFailure.Server(null))
                }
            }
        }
    }

    private fun loadFailed(failure: DataFailure) {
        generation?.cancelNow()
        mutableState.value = TripWizardUiState.LoadFailed(failure)
        java.util.logging.Logger
            .getLogger("Wizard")
            .warning("Wizard could not start")
        exitOnce()
    }

    suspend fun discard() {
        val model = wizard ?: return
        model.discard()
        if (model.closed) {
            discarded = true
            mutableState.value = TripWizardUiState.Closed(model.createdTripID)
        }
    }

    private fun exitOnce() {
        if (mutableExit.value == null) mutableExit.value = WizardExit.Trips
    }

    fun openAirport(sheet: WizardAirportSheet) {
        if (closing.value) return
        selectedAirport = false
        search.setQuery("")
        wizard?.openAirport(sheet)
    }

    fun cancelAirport() {
        if (closing.value) return
        search.setQuery("")
        wizard?.openAirport(null)
    }

    fun chooseAirport(
        airport: ApiAirportSuggestion,
        sheet: WizardAirportSheet,
    ) {
        val model = wizard ?: return
        if (closing.value || selectedAirport || model.draft.state.sheet != sheet ||
            !model.airportIsAllowed(airport.iata, sheet)
        ) {
            return
        }
        selectedAirport = true
        search.setQuery("")
        model.chooseAirport(airport, sheet)
    }

    fun swapAirports() {
        if (!closing.value) wizard?.swapAirports()
    }

    fun removeStop(routeIndex: Int) {
        if (!closing.value) wizard?.removeStop(routeIndex)
    }

    fun makeDirect(home: Boolean) {
        if (!closing.value) wizard?.makeDirect(home)
    }

    fun continueFrom(step: WizardStepKey) {
        val model = wizard ?: return
        if (closing.value || continuing || model.draft.state.step != step.step) return
        if ((step == WizardStepKey.Where && !model.whereComplete) ||
            (step == WizardStepKey.ReturnRoute && !model.returnRouteComplete)
        ) {
            return
        }
        continuing = true
        try {
            when (step) {
                WizardStepKey.Where -> model.finishRoute()
                WizardStepKey.ReturnRoute -> model.finishRoute(home = true)
                else -> Unit
            }
        } finally {
            continuing = false
        }
    }

    fun back() {
        if (closing.value) return
        val model = wizard ?: return
        val path = model.navigationPath
        model.popNavigation(path.dropLast(1), path)
    }

    fun requestClose() {
        if (closing.value) return
        if (wizard?.hasChanges == true) mutableDialog.value = true else close()
    }

    fun dismissDiscard() {
        if (!closing.value) mutableDialog.value = false
    }

    fun confirmDiscard() {
        if (closing.value) return
        mutableDialog.value = false
        close()
    }

    private fun close() {
        if (closing.value) return
        mutableClosing.value = true
        viewModelScope.launch {
            try {
                discard()
                exitOnce()
            } catch (_: CancellationException) {
                currentCoroutineContext().ensureActive()
                closeFailed()
            } catch (_: DataFailure) {
                closeFailed()
            } catch (_: SessionFailure) {
                closeFailed()
            } catch (_: DeviceIdentityFailure) {
                closeFailed()
            } catch (_: StoreFailure) {
                closeFailed()
            } catch (_: IOException) {
                closeFailed()
            } catch (_: GeneralSecurityException) {
                closeFailed()
            } catch (_: RuntimeException) {
                closeFailed()
            }
        }
    }

    private fun closeFailed() {
        mutableClosing.value = false
        java.util.logging.Logger
            .getLogger("Wizard")
            .warning("Wizard recovery discard failed")
    }

    fun onQuery(text: String) {
        if (!closing.value) search.setQuery(text)
    }

    fun retrySearch() {
        if (!closing.value) search.retry()
    }

    fun onEvent(event: WizardEvent) {
        when (event) {
            is WizardEvent.OpenAirport -> openAirport(event.sheet)
            WizardEvent.CancelAirport -> cancelAirport()
            is WizardEvent.ChooseAirport -> chooseAirport(event.airport, event.sheet)
            WizardEvent.SwapAirports -> swapAirports()
            is WizardEvent.RemoveStop -> removeStop(event.routeIndex)
            is WizardEvent.MakeDirect -> makeDirect(event.home)
            is WizardEvent.Continue -> continueFrom(event.step)
            WizardEvent.Back -> back()
            WizardEvent.RequestClose -> requestClose()
            WizardEvent.ConfirmDiscard -> confirmDiscard()
            WizardEvent.DismissDiscard -> dismissDiscard()
            is WizardEvent.Query -> onQuery(event.text)
            WizardEvent.RetrySearch -> retrySearch()
        }
    }

    override fun onCleared() {
        wizard?.close()
        generation?.cancelNow()
    }

    companion object {
        fun factory(entry: WizardEntry): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { TripWizardViewModel(entry, this[TripWizardDependenciesKey]) }
            }
    }
}
