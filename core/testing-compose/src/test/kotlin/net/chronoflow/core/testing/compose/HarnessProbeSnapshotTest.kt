package net.chronoflow.core.testing.compose

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.tiers.Snapshot
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Snapshot
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class HarnessProbeSnapshotTest {
    private val composeRule = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(composeRule)

    @Test
    fun probeMatchesItsStoredReference() {
        composeRule.setContent { HarnessProbe() }
        composeRule.onRoot().captureRoboImage("src/test/snapshots/HarnessProbe_default_pixel8_en_light.png")
    }
}
