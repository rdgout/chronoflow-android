package net.chronoflow.core.testing.compose

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.ui.window.LocalWindowSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WindowSizeFakeHostTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain =
        RuleChain
            .outerRule(
                ProfileRule(DeviceProfiles.foldInner.copy(heightDp = 914)),
            ).around(compose)

    @Test fun resizingKeepsTheCompositionAndUpdatesTheConstraints() {
        val fake = WindowSizeFake(DpSize(411.dp, 914.dp))
        var window = DpSize.Zero
        var constraints = DpSize.Zero
        var identity: Any? = null
        compose.setContent {
            WindowSizeFakeHost(fake) {
                window = LocalWindowSize.current
                identity = remember { Any() }
                BoxWithConstraints { constraints = DpSize(maxWidth, maxHeight) }
            }
        }
        compose.waitForIdle()
        val first = identity
        assertEquals(fake.size, window)
        assertEquals(fake.size.width.value, constraints.width.value, 0.5f / compose.density.density)
        assertEquals(fake.size.height.value, constraints.height.value, 0.5f / compose.density.density)
        compose.runOnIdle { fake.size = DpSize(841.dp, 914.dp) }
        compose.waitForIdle()
        assertEquals(fake.size, window)
        assertEquals(fake.size.width.value, constraints.width.value, 0.5f / compose.density.density)
        assertEquals(fake.size.height.value, constraints.height.value, 0.5f / compose.density.density)
        assertSame(first, identity)
    }
}
