package net.chronoflow.feature.launch

import android.graphics.Rect
import android.os.Bundle
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.launch.Gate
import net.chronoflow.core.model.launch.LaunchReadiness
import net.chronoflow.core.model.launch.SignedOutReadiness
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.launch.model.LaunchKind
import net.chronoflow.feature.launch.model.LaunchRoute
import net.chronoflow.feature.launch.model.ManualLaunchClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LaunchControllerLifecycleTest {
    @Test fun recreatedActivityKeepsTheEpochCoordinatorAndHandOff() =
        runTest(timeout = 60.seconds) {
            val clock = ManualLaunchClock()
            val source = LifecycleReadiness()
            val controller =
                LaunchController(moon = {
                    error("no drawing")
                }, processToken = "same", nanoTime = {
                    123L
                }, scope = backgroundScope, clockFactory = { clock }, readinessFactory = { source })
            controller.captureReduceMotion(true, false)
            assertEquals(LaunchKind.Cold, controller.onActivityCreated(null, false))
            controller.onOverlayGeometry(
                net.chronoflow.feature.launch.model
                    .LaunchSize(390.0, 844.0),
                net.chronoflow.feature.launch.model.MoonTarget
                    .Insets(0.0, 0.0, 0.0, 0.0),
                28.0,
            )
            controller.onFirstDraw(Rect())
            runCurrent()
            val coordinator = controller.coordinator
            val plan = controller.handOff
            val t0 = controller.t0Nanos
            controller.onActivityDestroyed(false, true)
            val bundle = Bundle().apply { putString(LaunchController.KEY_PROCESS_TOKEN, "same") }
            assertEquals(LaunchKind.Recreated, controller.onActivityCreated(bundle, false))
            runCurrent()
            assertSame(coordinator, controller.coordinator)
            assertSame(plan, controller.handOff)
            assertEquals(t0, controller.t0Nanos)
            assertEquals(1, source.sessions)
            assertTrue(controller.overlayActive)
            assertFalse(controller.overlayClaimedByCurrentActivity)
            assertFalse(controller.holdsSystemSplash())
            controller.onActivityDestroyed(true, false)
        }

    @Test fun finishingMidLaunchCompletesAndWarmKnownRouteDoesNotRestart() =
        runTest(timeout = 60.seconds) {
            val clock = ManualLaunchClock()
            val source = LifecycleReadiness()
            val controller =
                LaunchController(moon = {
                    error("no drawing")
                }, scope = backgroundScope, clockFactory = { clock }, readinessFactory = { source })
            controller.captureReduceMotion(true, false)
            controller.onActivityCreated(null, false)
            controller.onOverlayGeometry(
                net.chronoflow.feature.launch.model
                    .LaunchSize(390.0, 844.0),
                net.chronoflow.feature.launch.model.MoonTarget
                    .Insets(0.0, 0.0, 0.0, 0.0),
                28.0,
            )
            controller.onFirstDraw(Rect())
            runCurrent()
            assertTrue(clock.pendingSleeps > 0)
            controller.onActivityDestroyed(true, false)
            runCurrent()
            assertTrue(controller.handOffComplete)
            assertTrue(requireNotNull(controller.coordinator).state.isComplete)
            assertEquals(0, clock.pendingSleeps)
            assertEquals(LaunchKind.Warm, controller.onActivityCreated(null, false))
            runCurrent()
            assertEquals(LaunchRoute.Today, controller.route)
            assertEquals(1, source.sessions)
            assertFalse(controller.overlayActive)
        }

    @Test fun warmUnknownRouteUsesAHeadlessReduceMotionCoordinator() =
        runTest(timeout = 60.seconds) {
            val epochs = mutableListOf<Long>()
            val clocks = mutableListOf<ManualLaunchClock>()
            var now = 0L
            var factories = 0
            val controller =
                LaunchController(moon = {
                    error(
                        "no drawing",
                    )
                }, nanoTime = { now }, scope = backgroundScope, clockFactory = { epoch ->
                    epochs +=
                        epoch
                    ; ManualLaunchClock().also { clocks += it }
                }, readinessFactory = {
                    factories++
                    LifecycleReadiness(unresolvedSession = factories == 1, gate = Gate.Terms)
                })
            controller.captureReduceMotion(true, false)
            controller.onActivityCreated(null, false)
            controller.onOverlayGeometry(
                net.chronoflow.feature.launch.model
                    .LaunchSize(390.0, 844.0),
                net.chronoflow.feature.launch.model.MoonTarget
                    .Insets(0.0, 0.0, 0.0, 0.0),
                28.0,
            )
            controller.onFirstDraw(Rect())
            runCurrent()
            controller.onActivityDestroyed(true, false)
            runCurrent()
            now = 500_000_000
            assertEquals(LaunchKind.Warm, controller.onActivityCreated(null, false))
            runCurrent()
            assertEquals(listOf(0L, now), epochs)
            assertEquals(LaunchRoute.Terms, controller.route)
            assertEquals(Duration.ZERO, requireNotNull(controller.coordinator).state.exitStart)
            assertEquals(0, clocks.last().pendingSleeps)
            assertFalse(controller.overlayActive)
        }

    @Test fun screenProfileResolvesHeadlesslyWithoutHoldingSplash() =
        runTest(timeout = 60.seconds) {
            val clock = ManualLaunchClock()
            val controller =
                LaunchController(moon = { error("no drawing") }, scope = backgroundScope, clockFactory = { clock })
            controller.onActivityCreated(null, true)
            runCurrent()
            assertEquals(LaunchRoute.Welcome, controller.route)
            assertEquals(0, clock.pendingSleeps)
            assertFalse(controller.overlayActive)
            assertFalse(controller.handOffComplete)
            assertFalse(controller.holdsSystemSplash())
        }

    @Test fun warmStartBeforeTheColdSessionResolvesRoutesHeadlessly() =
        runTest(timeout = 60.seconds) {
            val clocks = mutableListOf<ManualLaunchClock>()
            var attempts = 0
            val controller =
                LaunchController(
                    moon = { error("no drawing") },
                    scope = backgroundScope,
                    clockFactory = { ManualLaunchClock().also { clocks += it } },
                    readinessFactory = {
                        attempts++
                        if (attempts == 1) LifecycleReadiness(unresolvedSession = true) else SignedOutReadiness
                    },
                )
            controller.captureReduceMotion(true, false)
            controller.onActivityCreated(null, false)
            controller.onOverlayGeometry(
                net.chronoflow.feature.launch.model
                    .LaunchSize(390.0, 844.0),
                net.chronoflow.feature.launch.model.MoonTarget
                    .Insets(0.0, 0.0, 0.0, 0.0),
                28.0,
            )
            controller.onFirstDraw(Rect())
            runCurrent()
            assertTrue(controller.overlayActive)
            assertNull(controller.route)
            assertEquals(LaunchKind.Warm, controller.onActivityCreated(null, false))
            runCurrent()
            assertEquals(2, attempts)
            assertEquals(LaunchRoute.Welcome, controller.route)
            assertEquals(Duration.ZERO, requireNotNull(controller.coordinator).state.exitStart)
            assertTrue(clocks.all { it.pendingSleeps == 0 })
            assertFalse(controller.overlayActive)
            assertFalse(controller.holdsSystemSplash())
            controller.onActivityDestroyed()
        }

    @Test fun recreatedReadyDiagnosticDoesNotStartReadiness() =
        assertDiagnosticRelaunchRemainsEmpty(LaunchOverrides(mode = LaunchMode.Ready), recreated = true)

    @Test fun recreatedSlowDiagnosticDoesNotStartReadiness() =
        assertDiagnosticRelaunchRemainsEmpty(LaunchOverrides(mode = LaunchMode.Slow), recreated = true)

    @Test fun warmReadyDiagnosticDoesNotStartReadiness() =
        assertDiagnosticRelaunchRemainsEmpty(LaunchOverrides(mode = LaunchMode.Ready), recreated = false)

    @Test fun warmSlowDiagnosticDoesNotStartReadiness() =
        assertDiagnosticRelaunchRemainsEmpty(LaunchOverrides(mode = LaunchMode.Slow), recreated = false)

    @Test fun warmFrozenLaunchDoesNotStartReadiness() =
        assertDiagnosticRelaunchRemainsEmpty(LaunchOverrides(freezeAt = 1.seconds), recreated = false)

    private fun assertDiagnosticRelaunchRemainsEmpty(
        overrides: LaunchOverrides,
        recreated: Boolean,
    ) = runTest(timeout = 60.seconds) {
        val clock = ManualLaunchClock()
        var readinessStarts = 0
        val controller =
            LaunchController(
                moon = { error("no drawing") },
                processToken = "same",
                nanoTime = { 0L },
                scope = backgroundScope,
                clockFactory = { clock },
                readinessFactory = {
                    readinessStarts++
                    SignedOutReadiness
                },
            )
        controller.captureReduceMotion(true, false)
        assertEquals(LaunchKind.Cold, controller.onActivityCreated(null, false, overrides))
        controller.onOverlayGeometry(
            net.chronoflow.feature.launch.model
                .LaunchSize(390.0, 844.0),
            net.chronoflow.feature.launch.model.MoonTarget
                .Insets(0.0, 0.0, 0.0, 0.0),
            28.0,
        )
        controller.onFirstDraw(Rect())
        runCurrent()
        assertNull(controller.route)
        assertNull(controller.coordinator)
        if (overrides.freezeAt != null) {
            controller.onActivityDestroyed()
        } else {
            controller.onExitFrame(5.seconds.inWholeNanoseconds)
        }
        val saved = if (recreated) Bundle().apply { putString(LaunchController.KEY_PROCESS_TOKEN, "same") } else null
        assertEquals(
            if (recreated) LaunchKind.Recreated else LaunchKind.Warm,
            controller.onActivityCreated(saved, false),
        )
        runCurrent()
        assertNull("diagnostic relaunch must keep its route empty", controller.route)
        assertNull(controller.coordinator)
        assertEquals(0, readinessStarts)
        assertFalse(controller.overlayActive)
        controller.onActivityDestroyed()
    }
}

private class LifecycleReadiness(
    private val unresolvedSession: Boolean = false,
    private val gate: Gate = Gate.App,
) : LaunchReadiness {
    var sessions = 0

    override suspend fun session(): SessionPresence {
        sessions++
        if (unresolvedSession) awaitCancellation()
        return SessionPresence.SignedIn("a")
    }

    override suspend fun gate() = gate

    override suspend fun hasStoredData() = false

    override suspend fun allowFirstLoad() = Unit
}
