package net.chronoflow.feature.launch.model

import net.chronoflow.core.model.launch.Gate
import net.chronoflow.core.model.launch.LaunchEnding.App
import net.chronoflow.core.model.launch.LaunchEnding.Welcome
import net.chronoflow.core.model.session.SessionPresence.SignedIn
import net.chronoflow.core.model.session.SessionPresence.SignedOut
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class LaunchReadinessTests(
    private val nonAppGate: Gate,
    private val signedOut: Boolean,
) {
    private val target = LaunchRect(38.0, 64.0, 28.0, 28.0)
    private var state = LaunchState(fallbackTarget = target)

    private fun step(
        event: LaunchEvent,
        rm: Boolean = false,
    ): List<LaunchEffect> {
        val result = reduce(state, event, rm)
        state = result.state
        return result.effects
    }

    private fun signedIn() = step(LaunchEvent.Session(SignedIn("a")))

    private fun reached(ms: Int) = step(LaunchEvent.Reached(ms.milliseconds))

    private fun appExit(ms: Int) = listOf(LaunchEffect.CommitExit(App, ms.milliseconds, null))

    private fun welcomeExit(
        ms: Int,
        rect: LaunchRect = target,
    ) = listOf(LaunchEffect.CommitExit(Welcome, ms.milliseconds, rect))

    @Test fun endingIsChosenOnceAndIntentNeverRoutes() {
        assertEquals(emptyList<LaunchEffect>(), step(LaunchEvent.Session(SignedOut)))
        step(LaunchEvent.Session(SignedIn("later")))
        assertEquals(Welcome, state.ending)
        assertEquals(LaunchRoute.Welcome, state.route)
        assertTrue(state.isReady)
    }

    @Test fun appWaitsForBothGateAndStoredDataThenCommitsAhead() {
        assertTrue(signedIn().isEmpty())
        assertTrue(step(LaunchEvent.StoredData(true)).isEmpty())
        assertFalse(state.isReady)
        assertEquals(appExit(1500), step(LaunchEvent.GateKnown(Gate.App)))
        assertEquals(LaunchRoute.Today, state.route)
        for (ms in listOf(1400, 1500, 2000, 8000, 9000)) assertTrue(reached(ms).isEmpty())
    }

    @Test fun nonAppGatesAreReadyWithoutStoredData() {
        signedIn()
        assertEquals(appExit(1500), step(LaunchEvent.GateKnown(nonAppGate)))
        assertTrue(state.isReady)
        assertEquals(nonAppGate.launchRoute(), state.route)
    }

    @Test fun lateReadyStartsAtReadinessAndStatusIsLatched() {
        signedIn()
        step(LaunchEvent.GateKnown(Gate.App))
        assertEquals(listOf(LaunchEffect.CommitStatus(2.seconds)), step(LaunchEvent.StoredData(false)))
        assertFalse(state.showsStatus)
        reached(2000)
        assertTrue(state.showsStatus)
        reached(3500)
        assertEquals(appExit(3500), step(LaunchEvent.StoredData(true)))
        assertTrue(state.showsStatus)
        assertEquals(2.seconds, state.statusStart)
    }

    @Test fun statusRequiresExplicitFalseAndCancelsBeforeItsStart() {
        signedIn()
        assertTrue(reached(1900).isEmpty())
        assertNull(state.statusStart)
        step(LaunchEvent.StoredData(false))
        assertEquals(listOf(LaunchEffect.CancelStatus) + appExit(1900), step(LaunchEvent.GateKnown(Gate.Terms)))
        reached(3000)
        assertFalse(state.showsStatus)
        assertNull(state.statusStart)
    }

    @Test fun lateStoredFalseStartsStatusNowAndNeverForUnknown() {
        signedIn()
        assertTrue(reached(4000).isEmpty())
        assertFalse(state.showsStatus)
        assertEquals(listOf(LaunchEffect.CommitStatus(4.seconds)), step(LaunchEvent.StoredData(false)))
        assertTrue(state.showsStatus)
    }

    @Test fun timeoutForcesCurrentSessionOnceAndIgnoresLateFacts() {
        signedIn()
        assertEquals(listOf(LaunchEffect.ForceReady) + appExit(8000), reached(8000))
        assertEquals(LaunchRoute.Today, state.route)
        assertTrue(reached(9000).isEmpty())
        assertTrue(step(LaunchEvent.GateKnown(Gate.Verify)).isEmpty())
        assertTrue(step(LaunchEvent.Session(SignedIn("b"))).isEmpty())
        assertEquals(SignedIn("a"), state.session)
    }

    @Test fun unresolvedSessionDefaultsSignedOutAtEightSeconds() {
        assertEquals(welcomeExit(8000), reached(8000))
        assertEquals(SignedOut, state.session)
        assertTrue(state.isReady)
        assertEquals(LaunchRoute.Welcome, state.route)
    }

    @Test fun welcomeCommitsOnAnchorAndRebuildsOnlyBeforeFlight() {
        step(LaunchEvent.Session(SignedOut))
        val first = LaunchRect(70.0, 90.0, 28.0, 28.0)
        assertEquals(welcomeExit(1600, first), step(LaunchEvent.MoonAnchor(first)))
        assertTrue(step(LaunchEvent.MoonAnchor(first)).isEmpty())
        reached(1599)
        assertEquals(welcomeExit(1600), step(LaunchEvent.MoonAnchor(target)))
        reached(1600)
        assertTrue(step(LaunchEvent.MoonAnchor(first)).isEmpty())
        assertEquals(target, state.moonTarget)
    }

    @Test fun welcomeFallbackIsOnlyCommittedAtDeadlineAndCanBeReplaced() {
        step(LaunchEvent.Session(SignedOut))
        assertTrue(reached(1549).isEmpty())
        assertEquals(welcomeExit(1600), reached(1550))
        val measured = LaunchRect(61.0, 103.0, 28.0, 28.0)
        assertEquals(welcomeExit(1600, measured), step(LaunchEvent.MoonAnchor(measured)))
        step(LaunchEvent.ExitCompleted)
        assertTrue(reached(8000).isEmpty())
        assertTrue(step(LaunchEvent.MoonAnchor(target)).isEmpty())
    }

    @Test fun invalidAnchorsAreIgnored() {
        step(LaunchEvent.Session(SignedOut))
        for (rect in listOf(
            LaunchRect(0.0, 0.0, 0.0, 0.0),
            LaunchRect(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 0.0, 0.0),
            LaunchRect(Double.NaN, 0.0, 28.0, 28.0),
            LaunchRect(0.0, Double.POSITIVE_INFINITY, 28.0, 28.0),
        )) {
            assertTrue(step(LaunchEvent.MoonAnchor(rect)).isEmpty())
            assertNull(state.moonTarget)
        }
    }

    @Test fun lateSignedOutStartsAtResolution() {
        reached(3000)
        assertEquals(welcomeExit(3000), step(LaunchEvent.Session(SignedOut)))
    }

    @Test fun reduceMotionSkipsWaitAndAnchor() {
        state = LaunchState()
        if (signedOut) {
            assertEquals(welcomeExit(0, LaunchRect(0.0, 0.0, 0.0, 0.0)), step(LaunchEvent.Session(SignedOut), true))
        } else {
            step(LaunchEvent.Session(SignedIn("a")), true)
            assertEquals(appExit(0), step(LaunchEvent.GateKnown(Gate.Terms), true))
        }
    }

    @Test fun gateRoutesMapLikeIos() {
        assertEquals(LaunchRoute.Verify, Gate.Verify.launchRoute())
        assertEquals(LaunchRoute.Terms, Gate.Terms.launchRoute())
        assertEquals(LaunchRoute.Walkthrough, Gate.Walkthrough.launchRoute())
        assertEquals(LaunchRoute.Today, Gate.App.launchRoute())
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}, signedOut={1}")
        fun cases(): List<Array<Any>> =
            listOf(arrayOf(Gate.Terms, true), arrayOf(Gate.Verify, false), arrayOf(Gate.Walkthrough, true))
    }
}
