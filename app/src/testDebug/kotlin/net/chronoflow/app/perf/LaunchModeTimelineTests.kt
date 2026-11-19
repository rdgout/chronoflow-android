package net.chronoflow.app.perf

import net.chronoflow.feature.launch.model.LaunchSize
import net.chronoflow.feature.launch.model.LaunchTimeline
import net.chronoflow.feature.launch.model.withReadyExit
import net.chronoflow.feature.launch.model.withStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class LaunchModeTimelineTests {
    @Test fun testModeTimelinesAndAsymmetricSafeAreaFallback() {
        val opening = LaunchTimeline.opening(LaunchSize(570.0, 730.0), false)
        val ready = LaunchDiagnosticsParser.overrides(mapOf("cf.mode" to "ready"))
        val slow = LaunchDiagnosticsParser.overrides(mapOf("cf.mode" to "slow"))
        assertEquals(opening.withReadyExit(1500.milliseconds).tracks, ready.timeline(opening).tracks)
        assertEquals(
            opening.withStatus(2.seconds).withReadyExit(4600.milliseconds).tracks,
            slow.timeline(opening).tracks,
        )
    }
}
