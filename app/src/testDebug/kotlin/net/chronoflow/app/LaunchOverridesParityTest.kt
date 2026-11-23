package net.chronoflow.app

import net.chronoflow.debug.DebugLaunchOverrides
import net.chronoflow.debug.TestSeed
import net.chronoflow.debug.UiTestProfile
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

class LaunchOverridesParityTest {
    @Test fun launchOverridesUseTheCanonicalSeedParser() {
        val values =
            mapOf(
                TestSeed.TEST_ID_KEY to "0199a3c0-0000-7000-8000-000000000000",
                TestSeed.PROFILE_KEY to "screen",
                "cf.reduceMotion" to "true",
                "cf.holdSplash" to "true",
                "cf.freezeAt" to "700",
            )
        val seed = TestSeed.parse(values)
        val overrides = DebugLaunchOverrides.fromSeed(seed)
        assertEquals(seed.reduceMotion, overrides.reduceMotion)
        assertEquals(seed.holdSplash == true, overrides.holdSplash)
        assertEquals(seed.freezeAtMs?.milliseconds, overrides.freezeAt)
        assertEquals(seed.profile == UiTestProfile.Screen, overrides.skipOverlay)
    }
}
