package net.chronoflow.core.designsystem

import net.chronoflow.core.designsystem.effect.PressValues
import net.chronoflow.core.designsystem.effect.pressValues
import net.chronoflow.core.designsystem.generated.PressTokens
import org.junit.Assert.assertEquals
import org.junit.Test

class CfPressValuesTest {
    @Test fun pressValuesRespectReduceMotion() {
        assertEquals(PressValues(0.97f, 0.85f, 120), pressValues(true, false))
        assertEquals(PressValues(1f, 0.85f, 0), pressValues(true, true))
        assertEquals(PressValues(1f, 1f, 120), pressValues(false, false))
        assertEquals(PressValues(1f, 1f, 0), pressValues(false, true))
        assertEquals(listOf(0.25f, 0.1f, 0.25f, 1f), PressTokens.curve.toList())
    }
}
