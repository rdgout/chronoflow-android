package net.chronoflow.feature.launch.model

import net.chronoflow.core.model.launch.LaunchEnding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class LaunchHandOffTimelineTests {
    private fun opening(rm: Boolean = false) = LaunchTimeline.opening(LaunchSize(390.0, 844.0), rm)

    private fun committed(
        ms: Int,
        ending: LaunchEnding = LaunchEnding.App,
    ) = LaunchState(ending = ending, didCommitExit = true, exitStart = ms.milliseconds)

    private fun end(
        ms: Int,
        rm: Boolean = false,
        ending: LaunchEnding = LaunchEnding.App,
    ) = LaunchHandOffTimeline.exitEndSeconds(LaunchHandOffTimeline.build(opening(rm), committed(ms, ending)))

    @Test fun buildStartsFromTheOpeningEveryTime() {
        val o = opening()
        val state = committed(4600).copy(statusStart = 2.seconds)
        assertEquals(
            o.withReadyExit(4600.milliseconds).withStatus(2.seconds).tracks,
            LaunchHandOffTimeline.build(o, state).tracks,
        )
        assertEquals(LaunchHandOffTimeline.build(o, state).tracks, LaunchHandOffTimeline.build(o, state).tracks)
        assertEquals(opening().tracks, o.tracks)
    }

    @Test fun exitEndIsNineteenHundredWhenReadyBefore1500() = assertEquals(1.9, end(0)!!, 0.0)

    @Test fun exitEndForLateReady() = assertEquals(3.9, end(3500)!!, 0.0)

    @Test fun exitEndWithReduceMotion() = assertEquals(0.35, end(50, true)!!, 0.0)

    @Test fun interimWelcomeEndsAtW950() {
        assertEquals(2.55, end(1600, ending = LaunchEnding.Welcome)!!, 1e-9)
        assertEquals(8.95, end(8000, ending = LaunchEnding.Welcome)!!, 1e-9)
        assertEquals(8.3, end(8000, true, LaunchEnding.Welcome)!!, 1e-9)
    }

    @Test fun interimWelcomeKeepsTheOpeningUntilW700() {
        val o = opening()
        val t = LaunchHandOffTimeline.build(o, committed(1600, LaunchEnding.Welcome))
        for (ms in 0..2300 step 10) assertEquals(o.sample(ms.milliseconds), t.sample(ms.milliseconds))
    }

    @Test fun noExitWhileNotCommitted() =
        assertNull(LaunchHandOffTimeline.exitEndSeconds(LaunchHandOffTimeline.build(opening(), LaunchState())))

    @Test fun cancelledStatusRemovesTheStatusTrack() {
        var state = LaunchState()
        state =
            reduce(
                state,
                LaunchEvent.Session(
                    net.chronoflow.core.model.launch.SessionPresence
                        .SignedIn("a"),
                ),
                false,
            ).state
        state = reduce(state, LaunchEvent.StoredData(false), false).state
        val o = opening()
        LaunchHandOffTimeline.build(o, state)
        state = reduce(state, LaunchEvent.GateKnown(net.chronoflow.core.model.launch.Gate.Terms), false).state
        val t = LaunchHandOffTimeline.build(o, state)
        assertEquals(
            listOf(0.0),
            t.tracks
                .single { it.layer == LayerID.Status }
                .keyTimes
                .toList(),
        )
    }
}
