package net.chronoflow.app

import org.junit.Assert.assertNull
import org.junit.Test

class RootOverridesReleaseTest {
    @Test fun releaseIgnoresTheReduceMotionSeed() {
        assertNull(RootOverrides.reduceMotion(null))
    }
}
