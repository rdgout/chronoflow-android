package net.chronoflow.core.wizard.model

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.contracts.ApiAirportSuggestion
import net.chronoflow.core.wizard.generation.MemberWizardGeneration
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

data class MemberWizardSnapshot(
    val draft: MemberWizardDraft,
    val closed: Boolean = false,
    val persistenceFailed: Boolean = false,
    val checking: WizardCheckingProgress = WizardCheckingProgress(),
    val retrySeconds: Int = 0,
    val choicePending: Boolean = false,
    val reviewing: Boolean = false,
    val createdTripID: String? = null,
    val saveOutcome: WizardSaveOutcome? = null,
    val guestAuthenticationPresented: Boolean = false,
)

class MemberWizardModel(
    draft: MemberWizardDraft,
    internal val store: WizardDraftStore,
    internal val scope: CoroutineScope,
    internal val now: () -> Instant,
    internal val generation: MemberWizardGeneration? = null,
    internal val checkingClock: WizardCheckingClock = WizardCheckingClock.continuous,
    writerScope: CoroutineScope = scope,
) {
    private val mutableSnapshots =
        MutableStateFlow(
            MemberWizardSnapshot(draft.copy(state = draft.state.discardRetiredValues()).enforceGuestMode()),
        )
    val snapshots: StateFlow<MemberWizardSnapshot> = mutableSnapshots.asStateFlow()
    var draft: MemberWizardDraft
        get() = mutableSnapshots.value.draft
        internal set(value) {
            mutableSnapshots.update { it.copy(draft = value) }
        }
    var closed: Boolean
        get() = mutableSnapshots.value.closed
        internal set(value) {
            mutableSnapshots.update { it.copy(closed = value) }
        }
    var persistenceFailed: Boolean
        get() = mutableSnapshots.value.persistenceFailed
        internal set(value) {
            mutableSnapshots.update { it.copy(persistenceFailed = value) }
        }
    var checking: WizardCheckingProgress
        get() = mutableSnapshots.value.checking
        internal set(value) {
            mutableSnapshots.update { it.copy(checking = value) }
        }
    var retrySeconds: Int
        get() = mutableSnapshots.value.retrySeconds
        internal set(value) {
            mutableSnapshots.update { it.copy(retrySeconds = value) }
        }
    var choicePending: Boolean
        get() = mutableSnapshots.value.choicePending
        internal set(value) {
            mutableSnapshots.update { it.copy(choicePending = value) }
        }
    var reviewing: Boolean
        get() = mutableSnapshots.value.reviewing
        internal set(value) {
            mutableSnapshots.update { it.copy(reviewing = value) }
        }
    var createdTripID: String?
        get() = mutableSnapshots.value.createdTripID
        internal set(value) {
            mutableSnapshots.update { it.copy(createdTripID = value) }
        }
    var saveOutcome: WizardSaveOutcome?
        get() = mutableSnapshots.value.saveOutcome
        internal set(value) {
            mutableSnapshots.update { it.copy(saveOutcome = value) }
        }
    var guestAuthenticationPresented: Boolean
        get() = mutableSnapshots.value.guestAuthenticationPresented
        internal set(value) {
            mutableSnapshots.update { it.copy(guestAuthenticationPresented = value) }
        }

    internal var checkJob: Job? = null
    internal var reviewJob: Job? = null
    internal var saveJob: Job? = null
    internal var choiceJob: Job? = null
    internal var retryJob: Job? = null
    private val writer = WizardRecoveryWriter(store, writerScope, onFailure = { persistenceFailed = true })
    internal var checkID: String = UUID.randomUUID().toString()
    internal var retryID: String = UUID.randomUUID().toString()
    internal var checkInstance: String? = null
    internal var resumed = false
    internal var state: MemberWizardState
        get() = draft.state
        set(value) {
            draft = draft.copy(state = value)
        }
    internal var form: WizardForm
        get() = draft.form
        set(value) {
            draft = draft.copy(form = value)
        }

    init {
        restartRetryCountdown()
    }

    val whereComplete: Boolean get() = form.routeComplete()
    val returnRouteComplete: Boolean get() = form.routeComplete(home = true)
    val outboundComplete: Boolean get() =
        form.legs.take(form.returnBoundary).all {
            WizardDateTime.date(it.departLocal) != null && WizardDateTime.date(it.arriveLocal) != null
        }

    fun go(step: MemberWizardStep) {
        if (closed || persistenceFailed || state.step == MemberWizardStep.calculating) return
        if (reviewing) cancelInlineReview()
        if (state.step == MemberWizardStep.checking && step != MemberWizardStep.checking) {
            cancelCheck()
            checking = WizardCheckingProgress()
        }
        choiceJob?.cancel()
        choicePending = false
        state =
            state.copy(
                progressFloor =
                    if (step in
                        listOf(
                            MemberWizardStep.checking,
                            MemberWizardStep.question,
                            MemberWizardStep.excluded,
                            MemberWizardStep.continuation,
                            MemberWizardStep.fineTune,
                            MemberWizardStep.ready,
                            MemberWizardStep.calculating,
                        )
                    ) {
                        progress
                    } else {
                        null
                    },
                step = step,
            )
        restartRetryCountdown()
        persist()
    }

    fun openAirport(sheet: WizardAirportSheet?) {
        if (closed) return
        state = state.copy(sheet = sheet)
        persist()
    }

    fun chooseAirport(
        airport: ApiAirportSuggestion,
        sheet: WizardAirportSheet,
    ) {
        if (closed || !airportIsAllowed(airport.iata, sheet)) return
        val home = sheet.index >= form.returnBoundary
        state = state.copy(airports = state.airports + (airport.iata to airport))
        if (sheet.side == WizardAirportSheet.Side.stop) {
            form =
                if (sheet.replacingStop ==
                    true
                ) {
                    form.setRouteAirport(airport, sheet.index + 1 - (if (home) form.returnBoundary else 0), home)
                } else {
                    form.insertStop(airport, if (home) form.legs.lastIndex else form.returnBoundary - 1)
                }
        } else {
            val position =
                if (sheet.side ==
                    WizardAirportSheet.Side.from
                ) {
                    0
                } else {
                    sheet.index + 1 - (if (home) form.returnBoundary else 0)
                }
            form = form.setRouteAirport(airport, position, home)
            if (sheet.side == WizardAirportSheet.Side.from) state = state.copy(fromLastTrip = false)
            if (home) state = state.copy(homeChanged = true)
            if (sheet.side == WizardAirportSheet.Side.from && form.end.kind == "return_legs" && !state.homeChanged) {
                form = form.updateLeg(form.legs.lastIndex) { it.copy(toIata = airport.iata, toCity = airport.city) }
            }
        }
        state = state.copy(sheet = null, pendingDateTimes = emptyMap())
        changed()
    }

    fun airportIsAllowed(
        iata: String,
        sheet: WizardAirportSheet,
    ): Boolean {
        if (sheet.index !in form.legs.indices) return false
        val home = sheet.index >= form.returnBoundary
        val route = form.route(home)
        if (sheet.side == WizardAirportSheet.Side.stop &&
            sheet.replacingStop != true
        ) {
            return route.takeLast(2).none { it.iata == iata }
        }
        val position =
            if (sheet.side ==
                WizardAirportSheet.Side.from
            ) {
                0
            } else {
                sheet.index + 1 - (if (home) form.returnBoundary else 0)
            }
        if (position !in route.indices || (home && position <= 0)) return false
        if (route.size == 2) return true
        return listOf(position - 1, position + 1).none { it in route.indices && route[it].iata == iata }
    }

    fun swapAirports() {
        val first = form.legs.firstOrNull() ?: return
        val last = form.legs.take(form.returnBoundary).lastOrNull() ?: return
        if (closed || form.returnBoundary != 1 || first.fromIata.isEmpty() || last.toIata.isEmpty()) return
        form =
            form
                .updateLeg(0) { it.copy(fromIata = last.toIata, fromCity = last.toCity) }
                .updateLeg(form.returnBoundary - 1) { it.copy(toIata = first.fromIata, toCity = first.fromCity) }
        if (form.end.kind == "return_legs" &&
            !state.homeChanged
        ) {
            form =
                form.updateLeg(form.legs.lastIndex) { it.copy(toIata = last.toIata, toCity = last.toCity) }
        }
        state = state.copy(fromLastTrip = false, pendingDateTimes = emptyMap())
        changed()
    }

    fun setDateTime(
        value: String,
        index: Int,
        arrival: Boolean,
    ) {
        if (closed || WizardDateTime.date(value) == null) return
        form = form.updateLeg(index) { if (arrival) it.copy(arriveLocal = value) else it.copy(departLocal = value) }
        changed()
    }

    fun removeStop(at: Int) {
        if (closed) return
        form = form.removeStop(at)
        state = state.copy(pendingDateTimes = emptyMap())
        changed()
    }

    fun makeDirect(home: Boolean = false) {
        if (closed) return
        form = form.makeDirect(home)
        state = state.copy(pendingDateTimes = emptyMap())
        changed()
    }

    fun finishReturnRoute() {
        if (closed || persistenceFailed || !returnRouteComplete) return
        state = state.copy(returnRouteConfirmed = true)
        go(MemberWizardStep.returnFlight)
    }

    fun finishOutbound(): Boolean {
        if (closed || persistenceFailed || !outboundComplete) return false
        draft = draft.copy(fieldErrors = WizardOutboundRules.errors(form, draft.airports, now(), false))
        persist()
        return draft.fieldErrors.isEmpty()
    }

    suspend fun flush() = writer.flush()

    fun close() = writer.close()

    fun setGuestAuthenticationPresented(presented: Boolean) {
        if (mode != WizardMode.guest) return
        guestAuthenticationPresented = presented
        saveOutcome = null
    }

    fun setGuestSigningIn(signingIn: Boolean) {
        if (mode != WizardMode.guest || closed) return
        state = state.copy(signingIn = signingIn)
        persist()
    }

    suspend fun discard() {
        if (closed || state.step == MemberWizardStep.calculating) return
        closed = true
        val jobs = listOfNotNull(checkJob, reviewJob, choiceJob, retryJob)
        restartRetryCountdown()
        cancelInlineReview()
        cancelCheck()
        choiceJob?.cancel()
        withContext(NonCancellable) {
            try {
                jobs.forEach { it.cancelAndJoin() }
                generation?.cancel()
                flush()
                store.discard(state.instance)
                close()
            } catch (failure: Exception) {
                closed = false
                restartRetryCountdown()
                throw failure
            }
        }
    }

    internal fun changed() {
        draft = draft.enforceGuestMode()
        saveOutcome = null
        draft = draft.copy(state = state.copy(entered = true), fieldErrors = emptyMap())
        persist()
    }

    internal fun persist() {
        state = state.copy(revision = state.revision + 1)
        writer.enqueue(draft)
    }

    fun minimumLocal(field: WizardFlightTiming.Field): LocalDateTime? =
        WizardFlightTiming(form, draft.airports).minimumLocal(field)

    fun initialLocal(field: WizardFlightTiming.Field): LocalDateTime =
        WizardFlightTiming(form, draft.airports).initialLocal(field, now())
}
