package net.chronoflow.feature.launch

import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flow
import net.chronoflow.core.model.launch.Gate
import net.chronoflow.core.model.launch.LaunchReadiness
import net.chronoflow.core.model.launch.SessionPresence
import net.chronoflow.feature.launch.model.ManualLaunchClock
import kotlin.time.Duration.Companion.milliseconds

internal class EndingReadiness(
    private val clock: ManualLaunchClock,
    private val readyMs: Int = 0,
    private val unresolvedSession: Boolean = false,
    private val unresolvedData: Boolean = false,
) : LaunchReadiness {
    var forced = 0

    override suspend fun session(): SessionPresence {
        if (unresolvedSession) awaitCancellation()
        return SessionPresence.SignedIn("a")
    }

    override suspend fun gate() = Gate.App

    override suspend fun hasStoredData() = readyMs == 0 && !unresolvedData

    override fun storedDataUpdates() =
        flow {
            if (unresolvedData) awaitCancellation()
            emit(readyMs == 0)
            if (readyMs != 0) {
                clock.sleepUntil(readyMs.milliseconds)
                emit(true)
            }
        }

    override suspend fun allowFirstLoad() {
        forced++
    }
}

internal class LaunchEndingTestHost(
    val compose: ComposeContentTestRule,
    private val readyMs: Int = 0,
    reduced: Boolean = false,
    unresolvedSession: Boolean = false,
    unresolvedData: Boolean = false,
) {
    private val frames = Channel<Long>(Channel.CONFLATED)
    val clock = ManualLaunchClock()
    val source = EndingReadiness(clock, readyMs, unresolvedSession, unresolvedData)
    var compositions = 0
    var generation by mutableIntStateOf(0)
    val frameLog = LaunchFrameLog()
    val controller =
        launchController(compose, ms = null, reduced = reduced, observer = {
            compositions++
        }, clockFactory = { clock }, readiness = { source }, nextFrame = { frames.receive() }, frameLog = frameLog)

    init {
        compose.mainClock.autoAdvance = false
        compose.setContent { if (controller.overlayActive) key(generation) { LaunchOverlay(controller) } }
        firstDraw(compose, controller)
    }

    fun advanceTo(ms: Int) {
        val end = controller.t0Nanos / 1_000_000 + ms
        while (compose.mainClock.currentTime < end) {
            var step = minOf(8L, end - compose.mainClock.currentTime)
            val untilReady = readyMs - (compose.mainClock.currentTime - controller.t0Nanos / 1_000_000)
            if (untilReady > 0) step = minOf(step, untilReady)
            val elapsed = compose.mainClock.currentTime - controller.t0Nanos / 1_000_000
            for (deadline in listOf(2000L, 8000L)) {
                if (deadline > elapsed) step = minOf(step, deadline - elapsed)
            }
            compose.mainClock.advanceTimeBy(step, ignoreFrameDuration = true)
            clock.advance((compose.mainClock.currentTime - controller.t0Nanos / 1_000_000).milliseconds)
            frames.trySend(7L)
            compose.waitForIdle()
        }
    }
}
