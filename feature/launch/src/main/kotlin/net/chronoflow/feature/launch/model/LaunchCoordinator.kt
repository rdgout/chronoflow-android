package net.chronoflow.feature.launch.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import net.chronoflow.core.model.launch.LaunchReadiness
import net.chronoflow.core.model.session.SessionPresence
import kotlin.time.Duration.Companion.seconds

class LaunchCoordinator(
    val readiness: LaunchReadiness,
    val clock: LaunchClock,
    private val reduceMotion: Boolean,
    initialState: LaunchState = LaunchState(),
    private val scope: CoroutineScope,
    private val onChange: (LaunchState, List<LaunchEffect>) -> Unit,
) {
    var state = initialState
        private set
    private val work = CoroutineScope(scope.coroutineContext + SupervisorJob(scope.coroutineContext[Job]))
    private var welcomeDeadlineJob: Job? = null
    private var started = false
    private var stopped = false

    fun start() {
        if (started || stopped) return
        started = true
        work.launch {
            val session = readiness.session()
            currentCoroutineContext().ensureActive()
            if (stopped || state.didForceReady) return@launch
            send(LaunchEvent.Session(session))
            if (!stopped && session is SessionPresence.SignedIn) {
                launch { readiness.gateUpdates().collect { send(LaunchEvent.GateKnown(it)) } }
                launch { readiness.storedDataUpdates().collect { send(LaunchEvent.StoredData(it)) } }
            }
        }
        for (deadline in listOf(2.seconds, 8.seconds)) {
            work.launch {
                clock.sleepUntil(deadline)
                send(LaunchEvent.Reached(clock.now))
            }
        }
    }

    fun send(event: LaunchEvent) {
        if (stopped) return
        val previous = state
        val reached = reduce(state, LaunchEvent.Reached(clock.now), reduceMotion)
        val step = reduce(reached.state, event, reduceMotion)
        state = step.state
        val effects = reached.effects + step.effects
        if (previous != state || effects.isNotEmpty()) onChange(state, effects)
        if (LaunchEffect.ForceReady in effects) scope.launch { readiness.allowFirstLoad() }
        if (stopped) return
        val deadline = state.welcomeDeadline
        if (previous.welcomeDeadline != deadline && deadline != null && !state.didCommitExit) {
            welcomeDeadlineJob?.cancel()
            welcomeDeadlineJob =
                work.launch {
                    clock.sleepUntil(deadline)
                    send(LaunchEvent.Reached(clock.now))
                }
        }
    }

    fun setFallbackTarget(rect: LaunchRect) {
        if (stopped) return
        state = state.copy(fallbackTarget = rect)
    }

    fun stop() {
        if (stopped) return
        stopped = true
        work.cancel()
    }
}
