package net.chronoflow.app

import net.chronoflow.debug.StrictModeViolationCounter
import org.junit.Assert.assertEquals
import org.junit.Test

class StrictModeViolationCounterTest {
    @Test fun counterUsesTheAppFrameFilterRegardlessOfLaunchGuardLifetime() {
        val path =
            net.chronoflow.app.diagnostics
                .LaunchPath()
                .apply { end() }
        org.junit.Assert.assertFalse(path.isActive)
        StrictModeViolationCounter.reset()
        StrictModeViolationCounter.record(false, listOf("net.chronoflow.feature.launch.welcome.WelcomeScreenKt"))
        StrictModeViolationCounter.record(true, listOf("android.content.res.Resources"))
        assertEquals(0, StrictModeViolationCounter.count)
        StrictModeViolationCounter.record(
            true,
            listOf("android.content.res.Resources", "net.chronoflow.feature.launch.welcome.WelcomeScreenKt"),
        )
        assertEquals(1, StrictModeViolationCounter.count)
        StrictModeViolationCounter.reset()
        assertEquals(0, StrictModeViolationCounter.count)
    }
}
