package net.chronoflow.feature.launch.model

import net.chronoflow.core.model.session.SessionPresence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class WelcomeIntroTimingTest(
    private val hz: Int,
) {
    private val size = LaunchSize(390.0, 844.0)
    private val target = LaunchRect(24.0, 64.0, 28.0, 28.0)

    private fun signedOut(
        at: Duration,
        rm: Boolean = false,
    ): LaunchState {
        val initial = LaunchState(now = at, fallbackTarget = target)
        val session = reduce(initial, LaunchEvent.Session(SessionPresence.SignedOut), rm).state
        return if (rm) {
            session
        } else {
            reduce(
                session,
                LaunchEvent.Reached(requireNotNull(session.welcomeDeadline)),
                rm,
            ).state
        }
    }

    private fun firstFrame(seconds: Double) =
        (0..(hz * 10)).first { it / hz.toDouble() + 1e-12 >= seconds } / hz.toDouble()

    @Test fun defaultAndLateSessionsReachBothMomentsOnTheFirstEligibleFrame() {
        for ((at, interactive, last) in listOf(10.milliseconds to (2.55 to 3.2), 4.seconds to (4.95 to 5.6)).map {
            Triple(it.first, it.second.first, it.second.second)
        }) {
            val state = signedOut(at)
            val timeline = LaunchHandOffTimeline.build(LaunchTimeline.opening(size, false), state)
            val intro = LaunchIntro(requireNotNull(state.exitStart) + 700.milliseconds, ManualLaunchClock())
            val end = firstFrame(requireNotNull(LaunchHandOffTimeline.exitEndSeconds(timeline)))
            val landed = (0..(hz * 10)).first { intro.progress(5, (it / hz.toDouble()).seconds) == 1.0 } / hz.toDouble()
            assertTrue(end >= interactive - 1e-12 && end <= interactive + 1.0 / hz)
            assertTrue(landed >= last - 1e-12 && landed <= last + 1.0 / hz)
            for ((index, nominal) in listOf(2750, 2850, 2900, 3050, 3150, 3200).withIndex()) {
                val first =
                    (0..(hz * 10)).first { intro.progress(index, (it / hz.toDouble()).seconds) == 1.0 } / hz.toDouble()
                assertTrue(first + 1e-12 >= nominal / 1000.0 + (last - 3.2))
            }
        }
    }

    @Test fun reduceMotionHandsOffAtSessionPlus300WithoutAnIntro() {
        val state = signedOut(120.milliseconds, true)
        assertEquals(120.milliseconds, state.exitStart)
        val t = LaunchHandOffTimeline.build(LaunchTimeline.opening(size, true), state)
        assertEquals(0.42, requireNotNull(LaunchHandOffTimeline.exitEndSeconds(t)), 1e-12)
    }

    @Test fun measuredAndDeadlineFallbackBothLandAt2300() {
        val session =
            reduce(
                LaunchState(fallbackTarget = target),
                LaunchEvent.Session(SessionPresence.SignedOut),
                false,
            ).state
        val measured =
            reduce(session.copy(now = 1400.milliseconds), LaunchEvent.MoonAnchor(target.offset(31.0, 16.0)), false)
        val fallback = reduce(session, LaunchEvent.Reached(1550.milliseconds), false)
        for (step in listOf(measured, fallback)) {
            assertTrue(step.effects.any { it is LaunchEffect.CommitExit })
            val t = LaunchHandOffTimeline.build(LaunchTimeline.opening(size, false), step.state)
            val frame = LaunchFrame()
            LaunchFrameSampler(t).sampleInto(2.3, frame)
            val destination = step.state.moonTarget ?: target
            assertEquals(destination.midX, frame.moonX, 1e-9)
            assertEquals(destination.midY, frame.moonY, 1e-9)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}Hz")
        fun refreshRates() = listOf(arrayOf(60), arrayOf(120))
    }
}
