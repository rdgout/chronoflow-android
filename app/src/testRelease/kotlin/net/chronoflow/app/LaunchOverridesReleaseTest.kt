package net.chronoflow.app

import net.chronoflow.feature.launch.LaunchOverrides
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class LaunchOverridesReleaseTest {
    @Test fun releaseHasNoLaunchOverridesOrResetHook() {
        assertEquals(LaunchOverrides.None, LaunchPlatform.overrides(null))
        assertFalse(AppGraph::class.java.declaredMethods.any { it.name.contains("resetLaunch") })
        assertThrows(ClassNotFoundException::class.java) { Class.forName("net.chronoflow.debug.DebugLaunchOverrides") }
    }
}
