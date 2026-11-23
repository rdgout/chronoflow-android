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
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.PromptKind
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.model.device.DeviceIdentityFailure
import net.chronoflow.core.model.session.SessionFailure
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.core.wizard.generation.MemberWizardGeneration
import net.chronoflow.core.wizard.model.AirportSearchService
import net.chronoflow.core.wizard.model.AirportSearchSession
import net.chronoflow.core.wizard.model.AirportSearchState
import net.chronoflow.core.wizard.model.MemberWizardModel
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.RecoveryRejected
import net.chronoflow.core.wizard.model.TripWizardAction
import net.chronoflow.core.wizard.model.WizardAirportSheet
import net.chronoflow.core.wizard.model.WizardClockRounding
import net.chronoflow.core.wizard.model.WizardDateTime
import net.chronoflow.core.wizard.model.WizardMode
import net.chronoflow.core.wizard.model.WizardSaveOutcome
import net.chronoflow.core.wizard.model.allowsSleepOverride
import net.chronoflow.core.wizard.model.answerOptional
import net.chronoflow.core.wizard.model.answerQuestion
import net.chronoflow.core.wizard.model.beginChecking
import net.chronoflow.core.wizard.model.changeExcludedAnswers
import net.chronoflow.core.wizard.model.checkingTitle
import net.chronoflow.core.wizard.model.chooseReturn
import net.chronoflow.core.wizard.model.credits
import net.chronoflow.core.wizard.model.editForm
import net.chronoflow.core.wizard.model.exclusionWasSaved
import net.chronoflow.core.wizard.model.finishFineTune
import net.chronoflow.core.wizard.model.finishFlightInput
import net.chronoflow.core.wizard.model.finishOutboundStep
import net.chronoflow.core.wizard.model.finishReturn
import net.chronoflow.core.wizard.model.finishRoute
import net.chronoflow.core.wizard.model.finishSleep
import net.chronoflow.core.wizard.model.firstScreen
import net.chronoflow.core.wizard.model.hasChanges
import net.chronoflow.core.wizard.model.isEditing
import net.chronoflow.core.wizard.model.navigationPath
import net.chronoflow.core.wizard.model.openPrompt
import net.chronoflow.core.wizard.model.optionalPrompts
import net.chronoflow.core.wizard.model.popNavigation
import net.chronoflow.core.wizard.model.preference
import net.chronoflow.core.wizard.model.progress
import net.chronoflow.core.wizard.model.readyTick
import net.chronoflow.core.wizard.model.resumeChecking
import net.chronoflow.core.wizard.model.resumeCompletion
import net.chronoflow.core.wizard.model.reviewInput
import net.chronoflow.core.wizard.model.revisit
import net.chronoflow.core.wizard.model.revisitGuestQuestions
import net.chronoflow.core.wizard.model.savePlan
import net.chronoflow.core.wizard.model.setDatePart
import net.chronoflow.core.wizard.model.setPreference
import net.chronoflow.core.wizard.model.setSleep
import net.chronoflow.core.wizard.model.setTimePart
import net.chronoflow.core.wizard.model.showsBack
import net.chronoflow.core.wizard.model.sleepComplete
import net.chronoflow.feature.wizard.answers.SUPPORTED_SHEET_KINDS
import net.chronoflow.feature.wizard.answers.WizardAnswerSheetState
import net.chronoflow.feature.wizard.checking.CheckingOutcome
import net.chronoflow.feature.wizard.checking.WizardHapticCue
import net.chronoflow.feature.wizard.finetune.promptLegIndex
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
    private val uiOverride = deps?.uiOverride
    private val signUpLauncher = deps?.signUpLauncher
    private val pickerNow = deps?.now ?: { Instant.EPOCH }
    private val mutableClosing = MutableStateFlow(false)
    val closing = mutableClosing.asStateFlow()
    private val mutableDialog = MutableStateFlow(presentation.discardDialogVisible)
    val discardDialogVisible = mutableDialog.asStateFlow()
    private val mutableExit = MutableStateFlow<WizardExit?>(null)
    private var savedHapticDelivered = false

    internal fun consumeSavedHaptic(): Boolean {
        if (mutableExit.value !is WizardExit.Saved || savedHapticDelivered) return false
        savedHapticDelivered = true
        return true
    }

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
    private var answerOpenedAt = Instant.EPOCH
    private var answerLanguage: Language = Language.En
    private val readyLanguage = MutableStateFlow<Language>(Language.En)
    private val mutableAnswer = MutableStateFlow<WizardAnswerSheetState?>(null)
    private val stepPresentation =
        combine(
            timesPresentation,
            mutableAnswer,
            readyLanguage,
        ) {
            times,
            answer,
            language,
            ->
            Triple(times, answer, language)
        }
    val uiState =
        combine(
            state,
            closing,
            discardDialogVisible,
            deps?.timeFormat ?: flowOf(TimeFormat.Value24),
            stepPresentation,
        ) { value, closing, dialog, timeFormat, stepPresentation ->
            val (times, answer, language) = stepPresentation
            (value as? TripWizardUiState.Active)?.let {
                val step = WizardStepKey(it.wizard.draft.state.step)
                WizardUiState
                    .from(
                        it,
                        closing,
                        dialog,
                        timeFormat,
                        times.second?.openedAt ?: answerOpenedAt,
                        times.first[step] ?: 0,
                        times.second?.picker,
                        openTime = times.third,
                        answerSheet = answer,
                        uiOverride = uiOverride,
                        language = language,
                    ).let { ui ->
                        ui.copy(
                            sheet = ui.sheet?.copy(seedQuery = presentation.query),
                            chrome =
                                ui.chrome.copy(
                                    showBack =
                                        if (uiOverride != null && step == WizardStepKey.Checking) {
                                            ui.chrome.showBack
                                        } else {
                                            wizard?.showsBack == true
                                        },
                                ),
                        )
                    }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val chrome =
        uiState.map { it?.chrome }.distinctUntilChanged().stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            null,
        )
    val where =
        uiState.map { it?.where }.distinctUntilChanged().stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            null,
        )
    val sheet =
        uiState.map { it?.sheet }.distinctUntilChanged().stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            null,
        )
    private var generation: MemberWizardGeneration? = null
    private val guestEntry = deps?.guest == true
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
                            WizardEntry.Create -> {
                                deps.start()
                            }

                            is WizardEntry.Edit -> {
                                deps.recover()?.takeIf { it.state.edit?.tripID == entry.tripId }
                                    ?: deps.startTrip(entry.tripId, TripWizardAction.edit)
                            }

                            is WizardEntry.Duplicate -> {
                                deps.recover()?.takeIf { it.state.edit == null && it.state.copiedRoute != null }
                                    ?: deps.startTrip(entry.tripId, TripWizardAction.duplicate)
                            }
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
                    if (uiOverride == null) {
                        model.resumeChecking()
                        model.resumeCompletion()
                    }
                    var previousSnapshot: net.chronoflow.core.wizard.model.MemberWizardSnapshot? = null
                    var cue: WizardHapticCue? = null
                    var previousStep: WizardStepKey? = null
                    model.snapshots.collect { snapshot ->
                        val previous = previousSnapshot
                        val outcome =
                            net.chronoflow.feature.wizard.checking
                                .checkingOutcome(previous, snapshot)
                        if (outcome != null) cue = WizardHapticCue(outcome, (cue?.sequence ?: 0) + 1)
                        previousSnapshot = snapshot
                        val nextStep =
                            WizardStepKey(
                                snapshot.draft.state.step,
                                snapshot.draft.state.questionIndex,
                            )
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
                            answerOpenedAt = pickerNow()
                            mutableAnswer.value =
                                WizardAnswerSheetState.load(
                                    prompt,
                                    model.reviewInput,
                                    snapshot.draft.state.savedProfile,
                                    snapshot.draft.fieldErrors,
                                    snapshot.draft.form,
                                    initialError = uiOverride?.answerError,
                                    language = answerLanguage,
                                )
                        }
                        val finished = snapshot.closed && snapshot.createdTripID != null
                        mutableState.value =
                            if (discarded || finished) {
                                TripWizardUiState
                                    .Closed(
                                        snapshot.createdTripID,
                                    ).also { exitOnce(snapshot.createdTripID) }
                            } else {
                                TripWizardUiState.Active(
                                    snapshot,
                                    model.progress,
                                    model.navigationPath,
                                    sleepComplete = model.sleepComplete,
                                    exclusionWasSaved = model.exclusionWasSaved,
                                    isEditing = model.isEditing,
                                    optionalPrompts = model.optionalPrompts,
                                    hasChanges = model.hasChanges,
                                    firstScreen = model.firstScreen,
                                    checkingTitle = model.checkingTitle,
                                    allowsSleepOverride = model.allowsSleepOverride,
                                    preferences =
                                        listOf("exclude_caffeine", "flight_movement_breaks")
                                            .associateWith(model::preference),
                                    cue = cue,
                                )
                            }
                        if (snapshot.saveOutcome == WizardSaveOutcome.signUpRequested &&
                            !snapshot.guestAuthenticationPresented && signUpLauncher != null
                        ) {
                            model.setGuestAuthenticationPresented(true)
                            model.flush()
                            if (model.closed || model.persistenceFailed) {
                                model.setGuestAuthenticationPresented(false)
                            } else {
                                signUpLauncher.request {
                                    if (viewModelScope.coroutineContext[kotlinx.coroutines.Job]?.isActive == true) {
                                        model.setGuestAuthenticationPresented(false)
                                        if (!model.closed) model.readyTick()
                                    }
                                }
                            }
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

    private fun exitOnce(tripId: String? = null) {
        if (mutableExit.value == null) {
            mutableExit.value =
                when {
                    tripId != null -> WizardExit.Saved(tripId)
                    guestEntry || wizard?.draft?.state?.mode == WizardMode.guest -> WizardExit.Welcome
                    else -> WizardExit.Trips
                }
        }
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
                WizardStepKey.Checking -> {
                    if (model.checking.failure != null) model.beginChecking()
                }

                WizardStepKey.Ready -> {
                    model.savePlan()
                }

                WizardStepKey.FineTune -> {
                    model.finishFineTune()
                }

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

    private fun closeFromFooter() {
        if (closing.value) return
        val model = wizard
        if (model?.draft?.state?.step == MemberWizardStep.excluded ||
            model?.draft?.state?.mode == WizardMode.guest || model?.hasChanges != true
        ) {
            close()
        } else {
            mutableDialog.value = true
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

    private fun setPreparationDays(requested: Int) {
        if (closing.value) return
        val model = wizard ?: return
        val days = requested.coerceIn(0, net.chronoflow.core.wizard.model.WizardForm.maxPreparationDays)
        if (days != model.draft.form.preparation.days) {
            model.editForm { it.copy(preparation = it.preparation.copy(days = days)) }
        }
    }

    private fun setSleepOverride(enabled: Boolean) {
        if (closing.value) return
        val model = wizard ?: return
        if (!model.allowsSleepOverride || model.draft.form.override.enabled == enabled) return
        val profile = model.draft.state.profile
        val saved = model.draft.state.savedProfile
        model.editForm { form ->
            val old = form.override
            form.copy(
                override =
                    old.copy(
                        enabled = enabled,
                        habitualBed =
                            if (enabled) {
                                old.habitualBed ?: profile["habitual_bed"].clockString()
                                    ?: saved?.get("habitualBed").clockString()
                            } else {
                                old.habitualBed
                            },
                        habitualWake =
                            if (enabled) {
                                old.habitualWake ?: profile["habitual_wake"].clockString()
                                    ?: saved?.get("habitualWake").clockString()
                            } else {
                                old.habitualWake
                            },
                    ),
            )
        }
    }

    fun onEvent(event: WizardEvent) {
        when (event) {
            is WizardEvent.RevisitFromReady -> {
                val model = wizard ?: return
                if (closing.value) return
                val row = event.row
                if (row == net.chronoflow.feature.wizard.ready.ReadyRow.Questions) {
                    model.revisitGuestQuestions()
                    return
                }
                val step =
                    when (row) {
                        net.chronoflow.feature.wizard.ready.ReadyRow.Route -> MemberWizardStep.whereTo
                        net.chronoflow.feature.wizard.ready.ReadyRow.Flight -> MemberWizardStep.outbound
                        net.chronoflow.feature.wizard.ready.ReadyRow.Return -> MemberWizardStep.returnChoice
                        else -> MemberWizardStep.fineTune
                    }
                if (model.draft.state.step != MemberWizardStep.ready || model.reviewing) return
                model.revisit(step, sleep = row == net.chronoflow.feature.wizard.ready.ReadyRow.Sleep)
                if (row == net.chronoflow.feature.wizard.ready.ReadyRow.LatestWake) {
                    model.optionalPrompts.firstOrNull { it.kind == PromptKind.LatestWake }?.let {
                        model.openPrompt(
                            it.id,
                        )
                    }
                }
            }

            WizardEvent.ReadyTick, WizardEvent.ReadyForeground -> {
                if (!closing.value && uiOverride == null) {
                    val model = wizard ?: return
                    model.readyTick(
                        force =
                            event == WizardEvent.ReadyForeground &&
                                model.credits == net.chronoflow.core.wizard.model.WizardCredits.unavailable,
                    )
                }
            }

            is WizardEvent.LanguageChanged -> {
                answerLanguage = event.language
                readyLanguage.value = event.language
                mutableAnswer.value = mutableAnswer.value?.withLanguage(event.language)
            }

            is WizardEvent.OpenPrompt -> {
                if (!closing.value) wizard?.openPrompt(event.id)
            }

            is WizardEvent.SetPreparationDays -> {
                setPreparationDays(event.days)
            }

            is WizardEvent.SetSleepOverride -> {
                setSleepOverride(event.enabled)
            }

            is WizardEvent.SetPreference -> {
                if (!closing.value) {
                    wizard?.setPreference(
                        event.field,
                        event.value,
                    )
                }
            }

            WizardEvent.CloseFromFooter -> {
                closeFromFooter()
            }

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
                            it.prompt.kind == PromptKind.FreeDayTimes &&
                                it.values["free_day_status"] == JsonPrimitive("declared") &&
                                event.field in setOf("free_day_bed", "free_day_wake")
                        } == true
                    val override =
                        model.draft.state.step == MemberWizardStep.fineTune && model.allowsSleepOverride &&
                            model.draft.form.override.enabled && !model.reviewing &&
                            event.field in setOf("override.habitual_bed", "override.habitual_wake")
                    val room =
                        mutableAnswer.value?.let { answer ->
                            val index = promptLegIndex(answer.prompt, model.draft.form)
                            answer.prompt.kind == PromptKind.Room &&
                                answer.values["stay.bed_available_mode"] == JsonPrimitive("declared") &&
                                event.field == "stay.bed_available_from_local" &&
                                model.draft.form.legs.getOrNull(index)?.arriveLocal?.let {
                                    WizardDateTime.date(it)
                                } != null
                        } == true
                    if (usual || free || override || room) mutableOpenTime.value = event.field
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
                    val override =
                        event.field in setOf("override.habitual_bed", "override.habitual_wake") &&
                            model.draft.state.step == MemberWizardStep.fineTune && model.allowsSleepOverride
                    if (override) {
                        mutableOpenTime.value = null
                        val value =
                            net.chronoflow.feature.wizard.sleep
                                .clockString(event.hour, event.minute)
                        val old = model.draft.form.override
                        val stored = if (event.field.endsWith("habitual_bed")) old.habitualBed else old.habitualWake
                        if (event.touched && value != stored) {
                            model.editForm {
                                it.copy(
                                    override =
                                        if (event.field.endsWith("habitual_bed")) {
                                            old.copy(habitualBed = value)
                                        } else {
                                            old.copy(habitualWake = value)
                                        },
                                )
                            }
                        }
                        return
                    }
                    if (event.field == "stay.bed_available_from_local" && answer != null) {
                        mutableOpenTime.value = null
                        if (event.touched) {
                            val index = promptLegIndex(answer.prompt, model.draft.form)
                            mutableAnswer.value =
                                answer.setRoomTime(
                                    clockString(event.hour, event.minute),
                                    model.draft.form.legs
                                        .getOrNull(index)
                                        ?.arriveLocal,
                                )
                        }
                        return
                    }
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
                    mutableAnswer.value =
                        answer.copy(
                            error = FieldMessage(error),
                            failures = answer.failures + 1,
                        )
                } else {
                    model.answerOptional(answer.prompt, answer.submittedValues())
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
