package net.chronoflow.feature.trip.page

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.CheckOffChange
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.device.DeviceIdentityFailure
import net.chronoflow.core.model.session.SessionFailure
import net.chronoflow.core.model.store.LocalMutationFailure
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.core.navigation.TripSheet
import net.chronoflow.core.plan.CopyFormat
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.plan.PlanClock
import net.chronoflow.core.plan.actions.TripActionsAvailability
import net.chronoflow.core.plan.drawing.DayDrawingCache
import net.chronoflow.core.plan.drawing.DayDrawingKey
import net.chronoflow.core.plan.drawing.TextMetrics
import net.chronoflow.core.plan.drawing.TimelineTraceNames
import net.chronoflow.core.plan.generated.TimelinePalette
import net.chronoflow.core.plan.step.StepBadge
import net.chronoflow.core.plan.step.StepSheetState
import net.chronoflow.core.plan.step.stepSheetState
import net.chronoflow.core.plan.trippage.TripPageAnchor
import net.chronoflow.core.plan.trippage.TripPageInputs
import net.chronoflow.core.plan.trippage.TripPageState
import net.chronoflow.core.plan.trippage.activeZone
import net.chronoflow.core.plan.trippage.dualClockValues
import net.chronoflow.core.plan.trippage.openingDay
import net.chronoflow.core.plan.trippage.projectDayClocks
import net.chronoflow.core.plan.trippage.replacementAnchor
import net.chronoflow.core.sync.PlanLease
import net.chronoflow.core.sync.SyncEvent
import net.chronoflow.core.sync.SyncPathMonitoring
import net.chronoflow.core.sync.SyncResult
import net.chronoflow.core.sync.SyncState
import net.chronoflow.core.sync.refresh.RefreshDebugProbe
import net.chronoflow.core.sync.refresh.RefreshFeedbackModel
import net.chronoflow.feature.trip.actions.TripActionsUi
import net.chronoflow.feature.trip.state.TripPageFetchModel
import net.chronoflow.feature.trip.timeline.DayClocks
import net.chronoflow.feature.trip.timeline.DrawingRequest
import net.chronoflow.feature.trip.timeline.TimelineDrawingRepository
import net.chronoflow.feature.trip.timeline.TimelineTracer
import net.chronoflow.feature.trip.timeline.host.FirstTimelineFrameTrace
import java.io.IOException
import java.security.GeneralSecurityException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

data class TripPageUi(
    val state: TripPageState,
    val title: TripTitleParts?,
    val plan: ReadyPlan?,
    val page: TripPageData = TripPageData(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class TripPageViewModel(
    val args: TripPageArgs,
    private val source: TripPageSource,
    private val clock: PlanClock,
    private val pipeline: TripPlanPipeline,
    private val savedState: SavedStateHandle,
    language: Language,
    private val compute: CoroutineDispatcher,
    buildDispatcher: CoroutineDispatcher,
    metrics: suspend () -> TextMetrics,
    cache: DayDrawingCache = DayDrawingCache(),
    private val formatter: suspend () -> CopyLineFormatter,
    private val tracer: TimelineTracer = TimelineTracer.Android,
    val refreshProbe: RefreshDebugProbe = RefreshDebugProbe.None,
    fetchDetail: suspend (String) -> Unit = {},
    makePath: () -> SyncPathMonitoring? = { null },
    syncEvents: Flow<SyncEvent> = emptyFlow(),
    epoch: Long = 0,
    initiallySyncCompleted: Boolean = true,
    initialTimeline: Boolean = true,
    private val initialSheet: TripSheet? = null,
    private val notificationInputHash: String? = null,
    private val isCurrent: () -> Boolean = { true },
    private val enqueueCheck: suspend (CheckOffChange) -> Unit = {},
    private val reportCheckFailure: (Throwable) -> Unit = {},
) : ViewModel() {
    private val checkOffWriter =
        net.chronoflow.core.data.plan
            .CheckOffWriter(enqueueCheck)
    private val lease: PlanLease

    init {
        tracer.beginAsync(TimelineTraceNames.FIRST_FRAME, FirstTimelineFrameTrace.COOKIE)
        lease = source.hold(args.tripId)
    }

    private val savedAnchor = TripPageAnchor(savedState[DAY], savedState[DATE], savedState[OFFSET] ?: 0.0)
    private val languageFlow = MutableStateFlow(language)
    private val inputSeq = AtomicLong()
    private val _ui = MutableStateFlow(TripPageUi(TripPageState.Loading, null, null))
    val ui: StateFlow<TripPageUi> = _ui.asStateFlow()
    private val _anchor = MutableStateFlow(savedAnchor)
    val anchor: StateFlow<TripPageAnchor> = _anchor.asStateFlow()
    private val _sheet = MutableStateFlow<TripSheet?>(null)
    val sheet: StateFlow<TripSheet?> = _sheet.asStateFlow()
    private val _sheetResolved = MutableStateFlow(initialSheet == null)
    val sheetResolved: StateFlow<Boolean> = _sheetResolved.asStateFlow()
    val selectedStep: StateFlow<TripSheet.Step?> =
        sheet
            .map { it as? TripSheet.Step }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val _checkBusy = MutableStateFlow(false)
    val checkBusy: StateFlow<Boolean> = _checkBusy.asStateFlow()

    private data class CheckExpectation(
        val blockId: String,
        val status: CheckStatus?,
    )

    private var pendingAnnouncement: CheckExpectation? = null
    private val announcementEvents = Channel<StepBadge?>(Channel.BUFFERED)
    val announcements: Flow<StepBadge?> = announcementEvents.receiveAsFlow()
    private val commands = Channel<ScrollCommand>(Channel.BUFFERED)
    val scrollCommands: Flow<ScrollCommand> = commands.receiveAsFlow()
    private val planFlow = MutableStateFlow<ReadyPlan?>(null)
    private var lastOutput: PipelineOutput? = null
    private val outputFlow = MutableStateFlow<PipelineOutput?>(null)
    private var hasLoaded = false
    private val awaitingSync = MutableStateFlow(args.awaitingSync)
    private val syncCompleted = MutableStateFlow(initiallySyncCompleted)
    private val fetchModel = TripPageFetchModel(args.tripId, fetchDetail, makePath(), viewModelScope)
    val online = fetchModel.online
    val stepSheet: StateFlow<StepSheetState?> =
        combine(sheet, outputFlow, clock.values()) { selected, output, now ->
            if (selected is TripSheet.Step) {
                withContext(compute) {
                    stepSheetState(
                        output?.plan?.detail,
                        selected.blockId,
                        now.toEpochMilli(),
                        formatter(),
                        output?.page?.language ?: language,
                        output?.page?.timeFormat ?: net.chronoflow.core.model.contracts.TimeFormat.Value24,
                    )
                }
            } else {
                null
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val tripActions: StateFlow<TripActionsUi?> =
        combine(ui, online) { page, connected ->
            val card = page.page.card
            val header = page.page.actionsHeader
            if (card == null || header == null) {
                null
            } else {
                TripActionsUi(
                    args.tripId,
                    header,
                    TripActionsAvailability(card.canEdit, card.canDuplicate, connected, false),
                )
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _timeline = MutableStateFlow(initialTimeline)
    val timeline: StateFlow<Boolean> = _timeline.asStateFlow()

    fun toggleView() {
        _timeline.value = !_timeline.value
    }

    fun retry() {
        fetchModel.retry()
    }

    private var offset = savedAnchor.offset
    private var opened = false
    private var firstOpen = true
    private val firstMetrics = AtomicBoolean(true)
    private val feedback = RefreshFeedbackModel(viewModelScope, refreshProbe)
    val refreshState = feedback.state

    fun dismissFeedback(id: Long) {
        feedback.dismiss(id)
    }

    val clocks = DayClocks()
    val summaryClock = SummaryClockState()
    val firstFrame = FirstTimelineFrameTrace(tracer)
    val repository =
        TimelineDrawingRepository(
            cache = cache,
            palette = { if (it) TimelinePalette.Dark else TimelinePalette.Light },
            metrics = {
                if (firstMetrics.getAndSet(false)) {
                    tracer.beginAsync(TimelineTraceNames.TYPEFACE, 3)
                    try {
                        metrics()
                    } finally {
                        tracer.endAsync(TimelineTraceNames.TYPEFACE, 3)
                    }
                } else {
                    metrics()
                }
            },
            buildDispatcher = buildDispatcher,
            scope = viewModelScope,
            tracer = tracer,
        )

    init {
        viewModelScope.launch {
            combine(source.snapshots(args.tripId), source.preferences(), languageFlow) { snapshot, preferences, lang ->
                PipelineInput(snapshot, preferences, lang) to inputSeq.incrementAndGet()
            }.mapLatest { (input, seq) ->
                val previous = planFlow.value
                withContext(compute) { pipeline.build(input, previous) } to seq
            }.filter { (_, seq) -> seq == inputSeq.get() }
                .collect { (output, _) ->
                    val previous = planFlow.value
                    output.plan?.let { next ->
                        clocks.retain(next.layouts.keys)
                        acceptPreparedPlan(previous, next)
                    }
                    if (output.plan == null) {
                        clocks.retain(emptySet())
                        summaryClock.apply(emptyList())
                    }
                    planFlow.value = output.plan
                    lastOutput = output
                    hasLoaded = true
                    if (output.trip != null) awaitingSync.value = false
                    outputFlow.value = output
                    recompute()
                }
        }
        viewModelScope.launch {
            combine(fetchModel.online, fetchModel.fetch, awaitingSync, syncCompleted) { _, _, _, _ -> Unit }
                .collect { recompute() }
        }
        viewModelScope.launch {
            syncEvents.collect { event ->
                if (event is SyncEvent.Finished && event.epoch == epoch && event.result.state == SyncState.Idle) {
                    syncCompleted.value = true
                }
            }
        }
    }

    suspend fun runFetch() {
        fetchModel.start()
        try {
            awaitCancellation()
        } finally {
            fetchModel.stop()
        }
    }

    private fun recompute() {
        val out = lastOutput ?: return
        val trip = out.trip
        val state =
            TripPageInputs(
                hasLoaded = hasLoaded,
                recordPresent = trip != null,
                keepOffline = trip?.keepOffline,
                tombstoned = trip?.deletedAt != null || trip?.deletedLocally == true,
                stored = out.plan != null,
                noPlan = out.noPlan,
                syncCompleted = syncCompleted.value,
                awaitingSync = awaitingSync.value,
                online = fetchModel.online.value,
                fetch = fetchModel.fetch.value,
            ).state()
        _ui.value = TripPageUi(state, out.title, out.plan, out.page)
        if (!_sheetResolved.value && state != TripPageState.Loading) {
            _sheet.value =
                when (val initial = initialSheet) {
                    is TripSheet.Step -> {
                        initial.takeIf {
                            state == TripPageState.Ready &&
                                out.plan
                                    ?.detail
                                    ?.plan
                                    ?.blocks
                                    ?.any { it.id == initial.blockId } == true
                        }
                    }

                    TripSheet.TripActions -> {
                        initial.takeIf { out.page.card != null }
                    }

                    else -> {
                        initial
                    }
                }
            _sheetResolved.value = true
        }
        if (state == TripPageState.Ready && notificationInputHash != null &&
            savedState.get<Boolean>(NOTIFICATION_HANDLED) != true
        ) {
            savedState[NOTIFICATION_HANDLED] = true
            if (out.plan
                    ?.detail
                    ?.plan
                    ?.meta
                    ?.inputHash == notificationInputHash
            ) {
                args.blockId?.let(::openStep)
            }
        }
        val pending = pendingAnnouncement
        if (pending != null) {
            val block =
                out.plan?.prepared?.days?.asSequence()?.flatMap { it.blocks.asSequence() }?.firstOrNull {
                    it.id ==
                        pending.blockId
                }
            if (block != null && block.status == pending.status) {
                pendingAnnouncement = null
                announcementEvents.trySend(
                    when (pending.status) {
                        CheckStatus.Done -> StepBadge.Done
                        null -> null
                        else -> StepBadge.Skipped
                    },
                )
            }
        }
    }

    private suspend fun acceptPreparedPlan(
        old: ReadyPlan?,
        next: ReadyPlan,
    ) {
        val days = next.detail.plan.days
        val current = anchor.value.copy(offset = offset)
        val value =
            if (!opened) {
                val now = withContext(compute) { clock.values().first() }
                val day = openingDay(days, next.detail.plan.blocks, args.dayId, args.blockId, savedAnchor.dayID, now)
                opened = true
                TripPageAnchor(day?.id, day?.labelDate, if (day?.id == savedAnchor.dayID) savedAnchor.offset else 0.0)
            } else {
                replacementAnchor(current, days)
            }
        if (old != null &&
            (
                old.prepared.days.map {
                    it.id
                } != next.prepared.days.map { it.id } || days.none { it.id == current.dayID }
            )
        ) {
            value.dayID?.let { commands.send(ScrollCommand(it, value.offset, false)) }
        }
        offset = value.offset
        _anchor.value = value
    }

    fun select(
        dayId: String,
        offsetDp: Double,
    ) {
        val day =
            planFlow.value
                ?.detail
                ?.plan
                ?.days
                ?.firstOrNull { it.id == dayId } ?: return
        offset = offsetDp
        if (dayId == anchor.value.dayID) return
        _anchor.value = TripPageAnchor(dayId, day.labelDate, offsetDp)
        saveAnchor()
    }

    fun saveAnchor() {
        savedState[DAY] = anchor.value.dayID
        savedState[DATE] = anchor.value.labelDate
        savedState[OFFSET] = offset
    }

    fun jump(dayId: String) {
        if (planFlow.value?.layouts?.containsKey(dayId) != true) return
        select(dayId, 0.0)
        commands.trySend(ScrollCommand(dayId, 0.0, true))
    }

    fun openingScroll(): TripPageAnchor? = if (opened) anchor.value.copy(offset = offset) else null

    fun consumeSummaryPosition(): Boolean = (firstOpen && args.startAtSummary).also { firstOpen = false }

    fun selectBlock(id: String) = openStep(id)

    fun openStep(blockId: String) {
        if (ui.value.state == TripPageState.Ready &&
            planFlow.value
                ?.detail
                ?.plan
                ?.blocks
                ?.any { it.id == blockId } == true
        ) {
            _sheet.value = TripSheet.Step(blockId)
        }
    }

    fun openLegend() {
        _sheet.value = TripSheet.Legend
    }

    fun openTripActions() {
        if (ui.value.page.card != null) _sheet.value = TripSheet.TripActions
    }

    fun dismissSheet() {
        _sheet.value = null
    }

    fun setCheck(
        blockId: String,
        status: CheckStatus?,
    ) {
        submitCheck(blockId, status, false)
    }

    fun checkFromAccessibility(
        blockId: String,
        status: CheckStatus?,
    ) {
        submitCheck(blockId, status, true)
    }

    private fun submitCheck(
        blockId: String,
        status: CheckStatus?,
        announce: Boolean,
    ) {
        if (_checkBusy.value || !isCurrent()) return
        val detail = planFlow.value?.detail ?: return
        val block = detail.plan.blocks.firstOrNull { it.id == blockId && it.actionable } ?: return
        if (block.checkKey == null) return
        _checkBusy.value = true
        if (announce) pendingAnnouncement = CheckExpectation(blockId, status)
        viewModelScope.launch {
            var saved = false
            try {
                checkOffWriter.setCheck(args.tripId, detail, blockId, status)
                saved = true
            } catch (cancelled: CancellationException) {
                if (!currentCoroutineContext().isActive) throw cancelled
                reportCheckFailure(cancelled)
            } catch (error: SessionFailure) {
                reportCheckFailure(error)
            } catch (error: DataFailure) {
                reportCheckFailure(error)
            } catch (error: DeviceIdentityFailure) {
                reportCheckFailure(error)
            } catch (error: StoreFailure) {
                reportCheckFailure(error)
            } catch (error: LocalMutationFailure) {
                reportCheckFailure(error)
            } catch (error: IOException) {
                reportCheckFailure(error)
            } catch (error: GeneralSecurityException) {
                reportCheckFailure(error)
            } catch (error: RuntimeException) {
                reportCheckFailure(error)
            } finally {
                if (!saved && announce) pendingAnnouncement = null
                _checkBusy.value = false
            }
        }
    }

    fun setLanguage(language: Language) {
        languageFlow.value = language
    }

    suspend fun refresh(): SyncResult? {
        var result: SyncResult? = null
        feedback.refresh { source.refresh().also { result = it } }?.join()
        return result
    }

    suspend fun runClock() {
        var lines: CopyLineFormatter? = null
        combine(outputFlow, clock.values()) { output, now -> output to now }.collect { (output, now) ->
            val plan = output?.plan
            if (plan == null && output?.page?.card == null) {
                clocks.apply(emptyMap())
                summaryClock.apply(emptyList())
                return@collect
            }
            val (dayValues, summaryValues) =
                withContext(compute) {
                    val format = lines ?: formatter().also { lines = it }
                    val options = CopyFormat(output.page.language, output.page.timeFormat, trip = plan?.detail?.trip)
                    val from = plan?.detail?.trip?.from ?: output.page.card!!.from
                    val to = plan?.detail?.trip?.to ?: output.page.card!!.to
                    projectDayClocks(plan?.layouts.orEmpty(), now, format, options) to
                        dualClockValues(
                            from,
                            to,
                            now,
                            plan?.let { activeZone(it.detail.plan.days, now) },
                            plan == null,
                            format,
                            options,
                        )
                }
            clocks.apply(dayValues)
            summaryClock.apply(summaryValues)
        }
    }

    fun prefetchAround(
        dayIndex: Int,
        key: DayDrawingKey,
    ) {
        val plan = planFlow.value ?: return
        val days = plan.prepared.days
        val requests = ArrayList<DrawingRequest>(4)
        for (i in dayIndex - 2..dayIndex + 2) {
            if (i == dayIndex || i !in days.indices) continue
            val day = days[i]
            requests.add(DrawingRequest(key.copy(dayId = day.id), day, plan.layouts.getValue(day.id)))
        }
        repository.prefetch(requests)
    }

    override fun onCleared() {
        fetchModel.stop()
        lease.release()
        commands.close()
        announcementEvents.close()
    }

    private companion object {
        const val NOTIFICATION_HANDLED = "trip.notificationStepHandled"
        const val DAY = "trip.anchor.day"
        const val DATE = "trip.anchor.labelDate"
        const val OFFSET = "trip.anchor.offset"
    }
}
