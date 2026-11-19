package net.chronoflow.feature.launch

import android.app.Application
import android.graphics.Rect
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.launch.Gate
import net.chronoflow.core.model.launch.LaunchReadiness
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.launch.model.LaunchRoute
import net.chronoflow.feature.launch.model.LaunchSize
import net.chronoflow.feature.launch.model.ManualLaunchClock
import net.chronoflow.feature.launch.model.MoonTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LaunchForcedExitTest {
    @Test fun unresolvedSessionForcesWelcomeAt8950() = checkForced(true, 8950, LaunchRoute.Welcome)

    @Test fun unresolvedStoredDataForcesTodayAt8400() = checkForced(false, 8400, LaunchRoute.Today)

    private fun checkForced(
        session: Boolean,
        end: Int,
        route: LaunchRoute,
    ) = runTest(timeout = 60.seconds) {
        val clock = ManualLaunchClock()
        val origin = 9_000_000_000_000L
        var wall = origin
        var forced = 0
        val frames = Channel<Unit>(Channel.UNLIMITED)
        val revisions = Channel<Unit>(Channel.CONFLATED)
        val source =
            object : LaunchReadiness {
                override suspend fun session(): SessionPresence =
                    if (session) awaitCancellation() else SessionPresence.SignedIn("a")

                override suspend fun gate() = Gate.App

                override suspend fun hasStoredData(): Boolean = awaitCancellation()

                override fun storedDataUpdates(): Flow<Boolean> = flow { awaitCancellation() }

                override suspend fun allowFirstLoad() {
                    forced++
                }
            }
        val controller =
            LaunchController(
                moon = { error("controller test does not draw") },
                nanoTime = { wall },
                tracing = LaunchTraceSession({ _, _ -> }, { _, _ -> }),
                scope = backgroundScope,
                clockFactory = { clock },
                readinessFactory = { source },
                onStateChange = { revisions.trySend(Unit) },
            )
        controller.captureReduceMotion(true, false)
        controller.onActivityCreated(null, false)
        val size = LaunchSize(411.0, 914.0)
        controller.onOverlayGeometry(size, MoonTarget.Insets(0.0, 0.0, 0.0, 0.0), 28.0)
        controller.setOpening(size)
        controller.onFirstDraw(Rect())
        val playback =
            backgroundScope.launch {
                runLaunchOpening(
                    controller.firstDrawSignal,
                    { wall },
                    {
                        frames.receive()
                        0L
                    },
                    settledNanos = 0L,
                    planProvider = { controller.handOff },
                    awaitRevision = { revisions.receive() },
                    onExit = controller::onExitFrame,
                    onElapsed = {},
                )
            }

        fun advanceTo(ms: Int) {
            wall = origin + ms * 1_000_000L
            clock.advance(ms.milliseconds)
            runCurrent()
            frames.trySend(Unit)
            runCurrent()
        }
        try {
            advanceTo(2000)
            val coordinator = checkNotNull(controller.coordinator)
            assertEquals(if (session) null else Gate.App, coordinator.state.gate)
            assertNull(coordinator.state.hasStoredData)
            assertFalse(controller.showsStatus)
            advanceTo(7900)
            assertFalse(coordinator.state.didCommitExit)
            assertFalse(controller.handOffComplete)
            advanceTo(8000)
            assertTrue(coordinator.state.didForceReady)
            assertEquals(route, controller.route)
            assertEquals(end / 1000.0, checkNotNull(controller.handOff.exitEndSeconds), 1e-9)
            assertEquals(if (session) 0 else 1, forced)
            advanceTo(end - 1)
            assertFalse(controller.handOffComplete)
            advanceTo(end)
            assertTrue(controller.handOffComplete)
            assertTrue(coordinator.state.isComplete)
            assertEquals(end * 1_000_000L, controller.handOffEndElapsedNanos)
            assertFalse(controller.overlayActive)
            assertTrue(playback.isCompleted)
        } finally {
            playback.cancel()
            controller.onActivityDestroyed()
        }
    }
}
