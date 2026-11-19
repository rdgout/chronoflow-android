package net.chronoflow.app

import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.testing.tiers.Smoke
import org.junit.Assert.assertEquals
import org.junit.Test

@Smoke
class LaunchHandOffTest : LaunchScreenTest() {
    @Test fun readyHandsOffAt1900() {
        resetLaunch()
        val scenario = launch(seed("cf.session" to "in", "cf.stored" to "yes", "cf.gate" to "app"))
        val frame = frameMillis(scenario)
        waitFor(5000) { read { it.handOffComplete } }
        assertEnd("ready", 1900.0, frame)
        compose.onNodeWithTag("route.today").assertExists()
        compose.onNodeWithTag("launch.opening").assertDoesNotExist()
    }

    @Test fun slowShowsStatusAndHandsOffAfterReady() {
        resetLaunch()
        val scenario = launch(seed("cf.session" to "in", "cf.stored" to "no", "cf.readyDelay" to "3.5"))
        val frame = frameMillis(scenario)
        waitFor(5000) { read { it.handOffComplete } }
        assertEquals(1, read { it.statusAnnouncementCount })
        assertEnd("slow", 3900.0, frame)
        compose.onNodeWithTag("route.today").assertExists()
        compose.onNodeWithTag("launch.opening").assertDoesNotExist()
    }
}
