package net.chronoflow.feature.auth.verify

import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalCoroutinesApi::class)
@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class VerifyLifecycleTest : VerifyTestFixture() {
    @Test fun startedActivatesEveryTimeAndOnlyStartedTimeTicksUntilDisposal() {
        host(Lifecycle.State.CREATED)
        assertEquals(0, checks)
        compose.runOnIdle { lifecycle.registry.currentState = Lifecycle.State.STARTED }
        flush()
        assertEquals(1, checks)
        val before = nowReads
        compose.runOnIdle {
            scheduler.advanceTimeBy(3_000)
            scheduler.runCurrent()
        }
        assertEquals(3, nowReads - before)
        compose.runOnIdle { lifecycle.registry.currentState = Lifecycle.State.CREATED }
        flush()
        val stopped = nowReads
        compose.runOnIdle {
            scheduler.advanceTimeBy(3_000)
            scheduler.runCurrent()
        }
        assertEquals(stopped, nowReads)
        compose.runOnIdle { lifecycle.registry.currentState = Lifecycle.State.STARTED }
        flush()
        assertEquals(2, checks)
        compose.runOnIdle { visible.value = false }
        flush()
        assertFalse(model.isActive)
    }
}
