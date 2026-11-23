package net.chronoflow.feature.launch

import androidx.compose.ui.test.junit4.v2.createComposeRule
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Layout
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LaunchOverlayPlacementTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)

    @Test fun layoutReleasesSplashBeforeFirstDraw() {
        val controller = launchController(compose)
        assertTrue(controller.holdsSystemSplash())
        compose.setContent { LaunchOverlay(controller) }
        firstDraw(compose, controller)
        assertTrue(controller.overlayPlaced)
        assertFalse(controller.holdsSystemSplash())
        assertTrue(controller.t0Nanos != LaunchController.UNSET)
    }
}
