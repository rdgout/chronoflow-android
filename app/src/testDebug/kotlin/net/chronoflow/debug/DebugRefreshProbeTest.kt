package net.chronoflow.debug

import org.junit.Assert.assertEquals
import org.junit.Test

class DebugRefreshProbeTest {
    @Test fun presentationsAreCountedAndResetBetweenSeeds() {
        try {
            DebugRefreshProbe.reset()
            DebugRefreshProbe.presented("fixture")
            assertEquals(1, DebugRefreshProbe.presentations.get())
            DebugRefreshProbe.reset()
            assertEquals(0, DebugRefreshProbe.presentations.get())
        } finally {
            DebugRefreshProbe.reset()
        }
    }
}
