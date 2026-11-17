package net.chronoflow.feature.launch.model

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.launch.Gate
import net.chronoflow.core.model.launch.LaunchEnding
import net.chronoflow.core.model.launch.LaunchReadiness
import net.chronoflow.core.model.launch.SessionPresence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private open class TestReadiness(
    private val presence: SessionPresence = SessionPresence.SignedIn("a"),
) : LaunchReadiness {
    var forced = 0

    override suspend fun session() = presence

    override suspend fun gate() = Gate.App

    override suspend fun hasStoredData() = false

    override suspend fun allowFirstLoad() {
        forced++
    }
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(Parameterized::class)
class LaunchCoordinatorTests(
    private val signedOut: Boolean,
) {
    @Test fun manualClockResumesAtDeadlinesAndCancelsSleep() =
        runTest(timeout = 60.seconds) {
            val clock = ManualLaunchClock()
            val early = backgroundScope.launch { clock.sleepUntil(2.seconds) }
            val late = backgroundScope.launch { clock.sleepUntil(8.seconds) }
            runCurrent()
            assertEquals(2, clock.pendingSleeps)
            clock.advance(2.seconds)
            runCurrent()
            assertTrue(early.isCompleted)
            assertEquals(1, clock.pendingSleeps)
            late.cancel()
            runCurrent()
            assertTrue(late.isCancelled)
            assertEquals(0, clock.pendingSleeps)
        }

    @Test fun coordinatorForcesOnlyItsSourceOnce() =
        runTest(timeout = 60.seconds) {
            val clock = ManualLaunchClock()
            val source = TestReadiness()
            val effects = mutableListOf<LaunchEffect>()
            val coordinator = LaunchCoordinator(source, clock, false, scope = backgroundScope) { _, e -> effects += e }
            coordinator.start()
            coordinator.start()
            runCurrent()
            assertEquals(false, coordinator.state.hasStoredData)
            clock.advance(8.seconds)
            runCurrent()
            assertEquals(1, source.forced)
            assertEquals(1, effects.filterIsInstance<LaunchEffect.CommitExit>().size)
            coordinator.send(LaunchEvent.Reached(9.seconds))
            runCurrent()
            assertEquals(1, source.forced)
            coordinator.stop()
            runCurrent()
            assertEquals(0, clock.pendingSleeps)
        }

    @Test fun forceReadySurvivesStopFromOnChange() =
        runTest(timeout = 60.seconds) {
            val clock = ManualLaunchClock()
            val source =
                object : TestReadiness() {
                    override suspend fun gate(): Gate = awaitCancellation()
                }
            lateinit var coordinator: LaunchCoordinator
            coordinator =
                LaunchCoordinator(source, clock, true, scope = backgroundScope) { state, _ ->
                    if (state.route != null) coordinator.stop()
                }
            coordinator.start()
            runCurrent()
            assertEquals(null, coordinator.state.route)
            assertEquals(0, source.forced)
            clock.advance(8.seconds)
            runCurrent()
            assertTrue(coordinator.state.didForceReady)
            assertEquals(LaunchRoute.Today, coordinator.state.route)
            assertEquals(0, clock.pendingSleeps)
            assertEquals(1, source.forced)
            clock.advance(9.seconds)
            coordinator.send(LaunchEvent.Reached(clock.now))
            coordinator.start()
            coordinator.stop()
            runCurrent()
            assertEquals(1, source.forced)
        }

    @Test fun stopCancelsPendingTimersBeforeAnyDeadline() =
        runTest(timeout = 60.seconds) {
            val clock = ManualLaunchClock()
            var changes = 0
            val presence = if (signedOut) SessionPresence.SignedOut else SessionPresence.SignedIn("a")
            val coordinator =
                LaunchCoordinator(TestReadiness(presence), clock, false, scope = backgroundScope) { _, _ -> changes++ }
            coordinator.start()
            runCurrent()
            assertEquals(if (signedOut) 3 else 2, clock.pendingSleeps)
            assertEquals(0.milliseconds, clock.now)
            coordinator.stop()
            val changesAtStop = changes
            val stateAtStop = coordinator.state
            runCurrent()
            assertEquals(0, clock.pendingSleeps)
            clock.advance(8.seconds)
            runCurrent()
            assertEquals(changesAtStop, changes)
            assertEquals(stateAtStop, coordinator.state)
            coordinator.start()
            runCurrent()
            assertEquals(0, clock.pendingSleeps)
        }

    @Test fun stopCancelsAnUnresolvedSessionTask() =
        runTest(timeout = 60.seconds) {
            val clock = ManualLaunchClock()
            var waiting = false
            var cancelled = false
            var changes = 0
            val source =
                object : TestReadiness() {
                    override suspend fun session(): SessionPresence {
                        waiting = true
                        try {
                            awaitCancellation()
                        } finally {
                            cancelled = true
                        }
                    }
                }
            val coordinator = LaunchCoordinator(source, clock, false, scope = backgroundScope) { _, _ -> changes++ }
            coordinator.start()
            runCurrent()
            assertTrue(waiting)
            assertEquals(2, clock.pendingSleeps)
            coordinator.stop()
            runCurrent()
            assertTrue(cancelled)
            assertEquals(0, clock.pendingSleeps)
            clock.advance(8.seconds)
            runCurrent()
            assertEquals(0, changes)
            assertEquals(null, coordinator.state.session)
            assertEquals(0, source.forced)
        }

    @Test fun signedOutUsesFallbackAtDeadlineAndFinishesOnce() =
        runTest(timeout = 60.seconds) {
            val clock = ManualLaunchClock()
            val target = LaunchRect(55.0, 76.0, 28.0, 28.0)
            val effects = mutableListOf<LaunchEffect>()
            val coordinator =
                LaunchCoordinator(
                    TestReadiness(SessionPresence.SignedOut),
                    clock,
                    false,
                    LaunchState(fallbackTarget = target),
                    backgroundScope,
                ) { _, e ->
                    effects +=
                        e
                }
            coordinator.start()
            runCurrent()
            assertTrue(coordinator.state.isReady)
            assertTrue(effects.isEmpty())
            assertEquals(3, clock.pendingSleeps)
            clock.advance(1550.milliseconds)
            runCurrent()
            assertEquals(listOf(LaunchEffect.CommitExit(LaunchEnding.Welcome, 1600.milliseconds, target)), effects)
            coordinator.send(LaunchEvent.ExitCompleted)
            coordinator.stop()
            val complete = coordinator.state
            coordinator.send(LaunchEvent.Session(SessionPresence.SignedIn("later")))
            assertEquals(complete, coordinator.state)
        }

    @Test fun storedDataStreamHandsOffAtReadinessBeforeTimeout() =
        runTest(timeout = 60.seconds) {
            val clock = ManualLaunchClock()
            val source =
                object : TestReadiness() {
                    override fun storedDataUpdates() =
                        flow {
                            emit(false)
                            clock.sleepUntil(3500.milliseconds)
                            emit(true)
                        }
                }
            val effects = mutableListOf<LaunchEffect>()
            val coordinator = LaunchCoordinator(source, clock, false, scope = backgroundScope) { _, e -> effects += e }
            coordinator.start()
            runCurrent()
            assertEquals(false, coordinator.state.hasStoredData)
            assertEquals(listOf(LaunchEffect.CommitStatus(2.seconds)), effects)
            clock.advance(2.seconds)
            runCurrent()
            assertTrue(coordinator.state.showsStatus)
            clock.advance(3500.milliseconds)
            runCurrent()
            assertEquals(3500.milliseconds, coordinator.state.exitStart)
            assertEquals(
                listOf(
                    LaunchEffect.CommitStatus(2.seconds),
                    LaunchEffect.CommitExit(LaunchEnding.App, 3500.milliseconds, null),
                ),
                effects,
            )
            assertFalse(coordinator.state.didForceReady)
            coordinator.stop()
        }

    @Test fun mediaClockUsesPassedCommitEpoch() {
        val n = 1_000_000_000L
        assertEquals(500.milliseconds, MediaLaunchClock(n - 500_000_000) { n }.now)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "signedOut={0}")
        fun cases() = listOf(arrayOf(false), arrayOf(true))
    }
}
