package net.chronoflow.app

import android.util.Log
import net.chronoflow.core.testing.tiers.Smoke
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

@Smoke
class LaunchStallTest : LaunchScreenTest() {
    @Test fun stallSkipsFramesAndNeverStretches() {
        resetLaunch()
        val scenario = launch(seed("cf.session" to "in", "cf.stored" to "yes", "cf.mainThreadStall" to "500"))
        val frame = frameMillis(scenario)
        waitFor(5000) { read { it.handOffComplete } }
        val frames =
            read { controller ->
                val log = requireNotNull(controller.frameLog)
                (maxOf(0, log.count - log.sampledNanos.size) until log.count).map { index ->
                    val slot = (index % log.sampledNanos.size).toInt()
                    log.sampledNanos[slot] / 1e6 to log.wallNanos[slot] / 1e6
                }
            }
        val index = frames.indexOfFirst { it.second >= 800 }
        assertTrue("no post-stall draw", index > 0)
        val after = frames[index]
        val before = frames[index - 1]
        val gap = after.first - before.first
        val error = abs(after.first - after.second)
        Log.i("CfLaunchProof", "stall sampledGapMs=$gap sampledVsWallMs=$error")
        assertTrue("post-stall sample differs by $error ms", error <= 2 * frame + 2)
        assertTrue("stall was stretched: gap $gap ms", gap >= 450)
        assertEnd("stall", 1900.0, frame)
    }
}
