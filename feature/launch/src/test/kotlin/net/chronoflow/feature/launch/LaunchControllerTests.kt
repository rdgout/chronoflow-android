package net.chronoflow.feature.launch

import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@Logic
class LaunchControllerTests {
    @Test fun claimOverlayReturnsTrueOnlyOnceAndNeverHoldsOnSecondActivity() {
        val controller = LaunchController(moon = { error("not decoded by a claim") })
        assertTrue(controller.claimOverlay(LaunchOverrides.None))
        assertTrue(controller.holdsSystemSplash())
        assertFalse(controller.claimOverlay(LaunchOverrides.None))
        assertFalse(controller.holdsSystemSplash())
    }

    @Test fun skippedActivityDoesNotClaimOrHoldSplash() {
        val controller = LaunchController(moon = { error("not decoded by a claim") })
        assertFalse(controller.claimOverlay(LaunchOverrides(skipOverlay = true)))
        assertFalse(controller.holdsSystemSplash())
        assertTrue(controller.claimOverlay(LaunchOverrides.None))
    }
}
