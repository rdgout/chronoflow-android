package net.chronoflow.feature.auth.verify

import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class VerifyCountdownRecompositionTest : VerifyTestFixture() {
    @Test fun tenTicksRecomposeOnlyTheCountdown() {
        host()
        act { model.resend() }
        val countdown = probe.countdownCompositions
        val buttons = probe.buttonCompositions

        fun tick() {
            compose.runOnIdle {
                now++
                model.tick()
            }
            flush()
        }
        tick()
        tick()
        tick()
        tick()
        tick()
        tick()
        tick()
        tick()
        tick()
        tick()
        assertEquals(10, probe.countdownCompositions - countdown)
        assertEquals(0, probe.buttonCompositions - buttons)
    }
}
