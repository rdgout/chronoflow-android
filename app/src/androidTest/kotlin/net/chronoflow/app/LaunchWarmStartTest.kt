package net.chronoflow.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import net.chronoflow.core.testing.tiers.Smoke
import net.chronoflow.feature.launch.model.LaunchKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

@Smoke
@OptIn(ExperimentalTestApi::class)
class LaunchWarmStartTest : LaunchScreenTest() {
    @Test fun warmStartShowsNoOverlay() {
        resetLaunch()
        val first = launch(seed("cf.session" to "in", "cf.stored" to "yes"))
        waitFor(5000) { read { it.handOffComplete } }
        first.onActivity { it.finish() }
        waitFor(2000) { first.state == Lifecycle.State.DESTROYED }
        ActivityScenario.launch(MainActivity::class.java).use {
            assertEquals(LaunchKind.Warm, read { launch -> requireNotNull(launch.lastLaunchKind) })
            compose.waitUntilDoesNotExist(hasTestTag("launch.opening"), 2000)
            compose.waitUntilExactlyOneExists(hasTestTag("route.today"), 2000)
            compose.onNodeWithTag("launch.opening").assertDoesNotExist()
            assertFalse(read { launch -> launch.holdsSystemSplash() })
            val fades = read { launch -> launch.warmFadeCount }
            if (fades != 0) assertEquals(1, fades)
        }
    }
}
