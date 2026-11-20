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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.contracts.ApiAirportSuggestion
import net.chronoflow.core.model.contracts.TimeFormat
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
import net.chronoflow.core.wizard.model.WizardClockRounding
import net.chronoflow.core.wizard.model.WizardMode
import net.chronoflow.core.wizard.model.answerOptional
import net.chronoflow.core.wizard.model.answerQuestion
import net.chronoflow.core.wizard.model.changeExcludedAnswers
import net.chronoflow.core.wizard.model.chooseReturn
import net.chronoflow.core.wizard.model.exclusionWasSaved
import net.chronoflow.core.wizard.model.finishFlightInput
import net.chronoflow.core.wizard.model.finishOutboundStep
import net.chronoflow.core.wizard.model.finishReturn
import net.chronoflow.core.wizard.model.finishRoute
import net.chronoflow.core.wizard.model.finishSleep
import net.chronoflow.core.wizard.model.hasChanges
import net.chronoflow.core.wizard.model.isEditing
import net.chronoflow.core.wizard.model.navigationPath
import net.chronoflow.core.wizard.model.openPrompt
import net.chronoflow.core.wizard.model.optionalPrompts
import net.chronoflow.core.wizard.model.popNavigation
import net.chronoflow.core.wizard.model.progress
import net.chronoflow.core.wizard.model.resumeChecking
import net.chronoflow.core.wizard.model.resumeCompletion
import net.chronoflow.core.wizard.model.reviewInput
import net.chronoflow.core.wizard.model.setDatePart
import net.chronoflow.core.wizard.model.setSleep
import net.chronoflow.core.wizard.model.setTimePart
import net.chronoflow.core.wizard.model.showsBack
import net.chronoflow.core.wizard.model.sleepComplete
import net.chronoflow.feature.wizard.answers.SUPPORTED_SHEET_KINDS
import net.chronoflow.feature.wizard.answers.WizardAnswerSheetState
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardExit
import net.chronoflow.feature.wizard.shell.WizardStepKey
import net.chronoflow.feature.wizard.sleep.clockString
import net.chronoflow.feature.wizard.times.WizardPicker
import net.chronoflow.feature.wizard.ui.AirportRowUi
import net.chronoflow.feature.wizard.ui.AirportSearchUi
import net.chronoflow.feature.wizard.ui.WizardInitialPresentation
import net.chronoflow.feature.wizard.ui.WizardInitialPresentationKey
import net.chronoflow.feature.wizard.ui.WizardUiState
import java.io.IOException
import java.security.GeneralSecurityException
import java.time.Duration
import java.time.Instant
import net.chronoflow.core.navigation.WizardMode as WizardEntry

class TripWizardViewModel(
    entry: WizardEntry,
    deps: TripWizardDependencies?,
    presentation: WizardInitialPresentation = WizardInitialPresentation(),
) : ViewModel() {
    private val mutableFormatter = MutableStateFlow<net.chronoflow.core.plan.CopyLineFormatter?>(null)
    val copyFormatter = mutableFormatter.asStateFlow()
    private val pickerNow = deps?.now ?: { Instant.EPOCH }
    private val mutableClosing = MutableStateFlow(false)
    val closing = mutableClosing.asStateFlow()
    private val mutableDialog = MutableStateFlow(presentation.discardDialogVisible)
    val discardDialogVisible = mutableDialog.asStateFlow()
    private val mutableExit = MutableStateFlow<WizardExit?>(null)
    val exit = mutableExit.asStateFlow()
    private var selectedAirport = false
    private var continuing = false
    private val mutableState = MutableStateFlow<TripWizardUiState>(TripWizardUiState.Loading)
    val state: StateFlow<TripWizardUiState> = mutableState.asStateFlow()
    private val continueFailures = MutableStateFlow<Map<WizardStepKey, Int>>(emptyMap())

    private data class OpenPicker(
        val picker: WizardPicker,
        val openedAt: Instant,
    )

    private val mutablePicker = MutableStateFlow<OpenPicker?>(null)
    private val mutableOpenTime = MutableStateFlow(presentation.timeSheet)
    private val timesPresentation =
        combine(continueFailures, mutablePicker, mutableOpenTime) { failures, picker, time ->
            Triple(failures, picker, time)
        }
    private val mutableAnswer = MutableStateFlow<WizardAnswerSheetState?>(null)
    private val stepPresentation = combine(timesPresentation, mutableAnswer) { times, answer -> times to answer }
    val uiState =
        combine(
            state,
            closing,
            discardDialogVisible,
            deps?.timeFormat ?: flowOf(TimeFormat.Value24),
            stepPresentation,
        ) { value, closing, dialog, timeFormat, stepPresentation ->
            val (times, answer) = stepPresentation
            (value as? TripWizardUiState.Active)?.let {
                val step = WizardStepKey(it.wizard.draft.state.step)
                WizardUiState
                    .from(
                        it,
                        closing,
                        dialog,
                        timeFormat,
                        times.second?.openedAt ?: Instant.EPOCH,
                        times.first[step] ?: 0,
                        times.second?.picker,
                        openTime = times.third,
                        answerSheet = answer,
                    ).let { ui ->
                        ui.copy(
                            sheet = ui.sheet?.copy(seedQuery = presentation.query),
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
                    mutableFormatter.value = deps.copyFormatter()
                } catch (_: CancellationException) {
                    currentCoroutineContext().ensureActive()
                } catch (_: IOException) {
                    // A later entry can retry a catalog that could not be read.
                } catch (_: RuntimeException) {
                    // Keep the iOS empty-copy fallback when the catalog cannot be decoded.
                }
            }
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
                    var previousStep: WizardStepKey? = null
                    model.snapshots.collect { snapshot ->
                        val nextStep = WizardStepKey(snapshot.draft.state.step, snapshot.draft.state.questionIndex)
                        if (previousStep != null && previousStep != nextStep) mutableOpenTime.value = null
                        previousStep = nextStep
                        val prompt =
                            model.optionalPrompts.firstOrNull {
                                it.id == snapshot.draft.state.promptSheetID && it.kind in SUPPORTED_SHEET_KINDS
                            }
                        if (prompt == null) {
                            mutableAnswer.value = null
                            if (mutableOpenTime.value in setOf("free_day_bed", "free_day_wake")) {
                                mutableOpenTime.value = null
                            }
                        } else if (mutableAnswer.value?.prompt?.id != prompt.id) {
                            mutableAnswer.value =
                                WizardAnswerSheetState.load(
                                    prompt,
                                    model.reviewInput,
                                    snapshot.draft.state.savedProfile,
                                    snapshot.draft.fieldErrors,
                                    snapshot.draft.form,
                                )
                        }
                        val finished = snapshot.closed && snapshot.createdTripID != null
                        mutableState.value =
                            if (discarded || finished) {
                                TripWizardUiState.Closed(snapshot.createdTripID).also { exitOnce() }
                            } else {
                                TripWizardUiState.Active(
                                    snapshot,
                                    model.progress,
                                    model.navigationPath,
                                    sleepComplete = model.sleepComplete,
                                    exclusionWasSaved = model.exclusionWasSaved,
                                    isEditing = model.isEditing,
                                    optionalPrompts = model.optionalPrompts,
                                )
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
        mutablePicker.value = null
        mutableOpenTime.value = null
        try {
            when (step) {
                WizardStepKey.Excluded -> {
                    model.changeExcludedAnswers()
                }

                WizardStepKey.Sleep -> {
                    if (!model.finishSleep()) {
                        continueFailures.update { it + (step to ((it[step] ?: 0) + 1)) }
                    }
                }

                WizardStepKey.Where -> {
                    model.finishRoute()
                }

                WizardStepKey.ReturnRoute -> {
                    model.finishRoute(home = true)
                }

                WizardStepKey.Outbound -> {
                    if (model.finishOutbound()) {
                        if (model.draft.state.revisit != null) model.finishFlightInput() else model.finishOutboundStep()
                    } else {
                        continueFailures.update { it + (step to ((it[step] ?: 0) + 1)) }
                    }
                }

                WizardStepKey.ReturnFlight -> {
                    if (model.finishReturn()) {
                        model.finishFlightInput()
                    } else {
                        continueFailures.update { it + (step to ((it[step] ?: 0) + 1)) }
                    }
                }

                else -> {}
            }
        } finally {
            continuing = false
        }
    }

    fun back() {
        if (closing.value) return
        if (mutableOpenTime.value != null) {
            mutableOpenTime.value = null
            return
        }
        if (mutableAnswer.value != null) {
            wizard?.openPrompt(null)
            return
        }
        if (mutablePicker.value != null) {
            mutablePicker.value = null
            return
        }
        val model = wizard ?: return
        val path = model.navigationPath
        model.popNavigation(path.dropLast(1), path)
    }

    fun requestClose() {
        if (closing.value) return
        val model = wizard
        if (model?.draft?.state?.mode == WizardMode.guest && model.draft.state.step == MemberWizardStep.excluded) {
            close()
        } else if (model?.hasChanges == true) {
            mutableDialog.value = true
        } else {
            close()
        }
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
        mutablePicker.value = null
        mutableOpenTime.value = null
        mutableAnswer.value = null
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

    private fun openFlightPicker(picker: WizardPicker) {
        val model = wizard ?: return
        if (closing.value || model.closed) return
        val form = model.draft.form
        val range =
            when (model.draft.state.step) {
                MemberWizardStep.outbound -> 0 until form.returnBoundary
                MemberWizardStep.returnFlight -> form.returnBoundary until form.legs.size
                else -> return
            }
        if (picker.index in range) mutablePicker.value = OpenPicker(picker, pickerNow())
    }

    fun onEvent(event: WizardEvent) {
        when (event) {
            WizardEvent.CloseNow -> {
                close()
            }

            is WizardEvent.AnswerQuestion -> {
                if (!closing.value) wizard?.answerQuestion(event.value)
            }

            is WizardEvent.SetSleep -> {
                if (!closing.value) wizard?.setSleep(event.field, event.value)
            }

            is WizardEvent.UpdateAnswer -> {
                if (!closing.value) {
                    mutableAnswer.value?.let { answer ->
                        if (event.field in
                            answer.prompt.fields
                        ) {
                            mutableAnswer.value = answer.update(event.field, event.value)
                        }
                    }
                }
            }

            WizardEvent.CancelAnswer -> {
                if (!closing.value) {
                    mutableOpenTime.value = null
                    wizard?.openPrompt(null)
                }
            }

            WizardEvent.AnswerDone -> {
                answerDone()
            }

            is WizardEvent.OpenTimeSheet -> {
                val model = wizard
                if (!closing.value && model != null) {
                    val usual =
                        model.draft.state.step == MemberWizardStep.sleep &&
                            event.field in setOf("habitual_bed", "habitual_wake")
                    val free =
                        mutableAnswer.value?.let {
                            it.prompt.kind == net.chronoflow.core.model.contracts.PromptKind.FreeDayTimes &&
                                it.values["free_day_status"] == JsonPrimitive("declared") &&
                                event.field in setOf("free_day_bed", "free_day_wake")
                        } == true
                    if (usual || free) mutableOpenTime.value = event.field
                }
            }

            WizardEvent.DismissTimeSheet -> {
                if (!closing.value) mutableOpenTime.value = null
            }

            is WizardEvent.ConfirmTimeSheet -> {
                val model = wizard
                if (!closing.value && model != null && mutableOpenTime.value == event.field) {
                    val usual =
                        event.field in setOf("habitual_bed", "habitual_wake") &&
                            model.draft.state.step == MemberWizardStep.sleep
                    val answer = mutableAnswer.value
                    val stored = if (usual) model.draft.state.profile[event.field] else answer?.values?.get(event.field)
                    val changed =
                        WizardClockRounding.change(
                            stored.clockString(),
                            event.hour,
                            event.minute,
                            event.touched,
                        )
                    mutableOpenTime.value = null
                    if (changed != null) {
                        if (usual) {
                            model.setSleep(event.field, JsonPrimitive(changed))
                        } else if (answer != null && event.field in answer.prompt.fields) {
                            mutableAnswer.value = answer.update(event.field, JsonPrimitive(changed))
                        }
                    }
                }
            }

            is WizardEvent.OpenFlightPicker -> {
                openFlightPicker(event.picker)
            }

            WizardEvent.DismissFlightPicker -> {
                mutablePicker.value = null
            }

            is WizardEvent.ChooseReturn -> {
                val model = wizard
                if (!closing.value && model != null &&
                    (
                        event.kind != "return_legs" || model.draft.form.end.kind == "return_legs" ||
                            model.draft.form.legs.size < net.chronoflow.core.wizard.model.WizardForm.maxFlights
                    )
                ) {
                    model.chooseReturn(event.kind)
                }
            }

            is WizardEvent.SetDatePart -> {
                if (!closing.value) {
                    wizard?.setDatePart(
                        event.index,
                        event.arrival,
                        event.date,
                    )
                }
            }

            is WizardEvent.SetTimePart -> {
                if (!closing.value) {
                    wizard?.setTimePart(
                        event.index,
                        event.arrival,
                        event.hour,
                        event.minute,
                    )
                }
            }

            is WizardEvent.OpenAirport -> {
                openAirport(event.sheet)
            }

            WizardEvent.CancelAirport -> {
                cancelAirport()
            }

            is WizardEvent.ChooseAirport -> {
                chooseAirport(event.airport, event.sheet)
            }

            WizardEvent.SwapAirports -> {
                swapAirports()
            }

            is WizardEvent.RemoveStop -> {
                removeStop(event.routeIndex)
            }

            is WizardEvent.MakeDirect -> {
                makeDirect(event.home)
            }

            is WizardEvent.Continue -> {
                continueFrom(event.step)
            }

            WizardEvent.Back -> {
                back()
            }

            WizardEvent.RequestClose -> {
                requestClose()
            }

            WizardEvent.ConfirmDiscard -> {
                confirmDiscard()
            }

            WizardEvent.DismissDiscard -> {
                dismissDiscard()
            }

            is WizardEvent.Query -> {
                onQuery(event.text)
            }

            WizardEvent.RetrySearch -> {
                retrySearch()
            }
        }
    }

    private var answering = false

    private fun answerDone() {
        if (closing.value || answering) return
        val model = wizard ?: return
        val answer = mutableAnswer.value ?: return
        answering = true
        viewModelScope.launch {
            try {
                if (closing.value || model.draft.state.promptSheetID != answer.prompt.id) return@launch
                val error = answer.validate()
                if (error != null) {
                    mutableAnswer.value = answer.copy(error = FieldMessage(error), failures = answer.failures + 1)
                } else {
                    model.answerOptional(answer.prompt, answer.values)
                }
            } catch (_: CancellationException) {
                currentCoroutineContext().ensureActive()
                mutableAnswer.value = answer.copy(error = FieldMessage("Choose one of the available options."))
            } catch (_: DataFailure) {
                mutableAnswer.value = answer.copy(error = FieldMessage("Choose one of the available options."))
            } catch (_: RuntimeException) {
                mutableAnswer.value = answer.copy(error = FieldMessage("Choose one of the available options."))
            } finally {
                answering = false
            }
        }
    }

    override fun onCleared() {
        wizard?.close()
        generation?.cancelNow()
    }

    companion object {
        fun factory(entry: WizardEntry): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    TripWizardViewModel(
                        entry,
                        this[TripWizardDependenciesKey],
                        this[WizardInitialPresentationKey] ?: WizardInitialPresentation(),
                    )
                }
            }
    }
}
