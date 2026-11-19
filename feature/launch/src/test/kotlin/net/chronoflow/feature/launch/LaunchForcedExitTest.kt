package net.chronoflow.feature.launch

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.launch.Gate
import net.chronoflow.core.model.launch.LaunchReadiness
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.feature.launch.model.LaunchCoordinator
import net.chronoflow.feature.launch.model.LaunchEvent
import net.chronoflow.feature.launch.model.LaunchHandOffTimeline
import net.chronoflow.feature.launch.model.LaunchRect
import net.chronoflow.feature.launch.model.LaunchRoute
import net.chronoflow.feature.launch.model.LaunchSize
import net.chronoflow.feature.launch.model.LaunchState
import net.chronoflow.feature.launch.model.LaunchTimeline
import net.chronoflow.feature.launch.model.ManualLaunchClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LaunchForcedExitTest {
    @Test fun unresolvedSessionForcesWelcomeAt8950() = checkForced(true, 8.950, LaunchRoute.Welcome)

    @Test fun unresolvedStoredDataForcesTodayAt8400() = checkForced(false, 8.400, LaunchRoute.Today)

    private fun checkForced(
        session: Boolean,
        end: Double,
        route: LaunchRoute,
    ) = runTest(timeout = 60.seconds) {
        val clock = ManualLaunchClock()
        var forced = 0
        val source =
            object : LaunchReadiness {
                override suspend fun session(): SessionPresence =
                    if (session) awaitCancellation() else SessionPresence.SignedIn("a")

                override suspend fun gate() = Gate.App

                override suspend fun hasStoredData() = false

                override suspend fun allowFirstLoad() {
                    forced++
                }
            }
        val coordinator =
            LaunchCoordinator(
                source,
                clock,
                false,
                LaunchState(fallbackTarget = LaunchRect(55.0, 76.0, 28.0, 28.0)),
                scope = backgroundScope,
            ) { _, _ -> }
        coordinator.start()
        runCurrent()
        clock.advance(7.9.seconds)
        runCurrent()
        assertFalse(coordinator.state.didCommitExit)
        clock.advance(8.seconds)
        runCurrent()
        assertTrue(coordinator.state.didCommitExit)
        assertEquals(route, coordinator.state.route)
        val timeline =
            LaunchHandOffTimeline.build(
                LaunchTimeline.opening(LaunchSize(411.0, 914.0), false),
                coordinator.state,
            )
        assertEquals(end, requireNotNull(LaunchHandOffTimeline.exitEndSeconds(timeline)), 1e-9)
        assertEquals(if (session) 0 else 1, forced)
        coordinator.send(LaunchEvent.ExitCompleted)
        assertTrue(coordinator.state.isComplete)
        coordinator.stop()
    }
}
