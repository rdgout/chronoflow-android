package net.chronoflow.app.perf

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import net.chronoflow.app.StubConfig
import net.chronoflow.app.StubSession
import net.chronoflow.app.StubStored
import net.chronoflow.core.model.launch.LaunchReadiness
import net.chronoflow.core.model.launch.SessionPresence
import net.chronoflow.core.model.launch.SignedOutReadiness
import net.chronoflow.feature.launch.model.LaunchClock
import kotlin.time.Duration.Companion.seconds

class StubReadiness(
    private val config: StubConfig,
    private val clock: LaunchClock,
    private val nativeReadiness: LaunchReadiness = SignedOutReadiness,
) : LaunchReadiness {
    var firstLoadCount = 0
        private set

    override suspend fun session(): SessionPresence =
        when (config.session) {
            StubSession.Native -> {
                nativeReadiness.session()
            }

            StubSession.SignedIn -> {
                SessionPresence.SignedIn("debug")
            }

            StubSession.SignedOut -> {
                SessionPresence.SignedOut
            }

            StubSession.Never -> {
                delay(60.seconds)
                SessionPresence.SignedOut
            }
        }

    override suspend fun gate() = config.gate

    override suspend fun hasStoredData(): Boolean {
        if (config.stored == StubStored.Never) delay(60.seconds)
        return config.stored == StubStored.Yes
    }

    override fun storedDataUpdates() =
        flow {
            if (config.stored == StubStored.Never) awaitCancellation()
            emit(config.stored == StubStored.Yes)
            if (config.stored == StubStored.No) {
                clock.sleepUntil(config.readyDelay)
                emit(true)
            }
        }

    override suspend fun allowFirstLoad() {
        if (firstLoadCount == 0) firstLoadCount = 1
    }
}
