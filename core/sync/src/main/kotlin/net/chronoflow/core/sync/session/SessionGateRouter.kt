package net.chronoflow.core.sync.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import net.chronoflow.core.model.launch.Gate
import net.chronoflow.core.model.launch.LaunchReadiness
import net.chronoflow.core.model.session.SessionPresence

class SessionGateRouter(
    private val controller: SessionController,
    private val readiness: LaunchReadiness,
    private val scope: CoroutineScope,
    private val onSignedOut: () -> Unit,
    private val onGate: (Gate) -> Unit,
    private val onVerify: suspend (Long) -> Unit,
) {
    private var sessionTask: Job? = null
    private var gateJob: Job? = null

    fun start() {
        if (sessionTask != null) return
        sessionTask =
            scope.launch {
                var sawSignedIn = false
                controller.updates.filterNotNull().collect { update ->
                    gateJob?.cancel()
                    gateJob = null
                    when (update.presence) {
                        SessionPresence.SignedOut -> {
                            if (sawSignedIn && controller.epoch == update.epoch) onSignedOut()
                        }

                        is SessionPresence.SignedIn -> {
                            sawSignedIn = true
                            val snapshot = controller.activeSession()
                            if (snapshot != null && snapshot.epoch == update.epoch) {
                                gateJob =
                                    scope.launch {
                                        readiness
                                            .gateUpdates()
                                            .takeWhile { controller.activeSession() == snapshot }
                                            .distinctUntilChanged()
                                            .collect { gate ->
                                                if (gate == Gate.Verify) onVerify(update.epoch) else onGate(gate)
                                            }
                                    }
                            }
                        }
                    }
                }
            }
    }

    fun stop() {
        sessionTask?.cancel()
        sessionTask = null
        gateJob?.cancel()
        gateJob = null
    }
}
