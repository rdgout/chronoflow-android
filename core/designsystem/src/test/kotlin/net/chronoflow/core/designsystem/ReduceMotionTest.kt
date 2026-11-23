package net.chronoflow.core.designsystem

import net.chronoflow.core.designsystem.theme.ReduceMotion
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReduceMotionTest {
    @Test fun overrideWinsOverTheSystemAnimationSetting() {
        assertTrue(ReduceMotion.resolve(false, null))
        assertFalse(ReduceMotion.resolve(true, null))
        assertTrue(ReduceMotion.resolve(true, true))
        assertTrue(ReduceMotion.resolve(false, true))
        assertFalse(ReduceMotion.resolve(true, false))
        assertFalse(ReduceMotion.resolve(false, false))
    }
}
