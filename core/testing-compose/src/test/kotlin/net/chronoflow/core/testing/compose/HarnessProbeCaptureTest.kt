package net.chronoflow.core.testing.compose

import androidx.compose.ui.test.junit4.v2.createComposeRule
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.tiers.Capture
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Capture
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class HarnessProbeCaptureTest {
    private val composeRule = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(composeRule)

    @Test
    fun capturesTheProbe() {
        composeRule.setContent { HarnessProbe() }
        BoardCapture.capture(composeRule, "harness/HarnessProbe_pixel8_en_light")
    }
}
