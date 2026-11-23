package net.chronoflow.app

import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class WelcomeDestinationsReleaseTest {
    @Test fun releaseWelcomeActionsRemainInert() {
        val destinations = LaunchPlatform.welcomeDestinations
        assertSame(InertWelcomeDestinations, destinations)
        destinations.planMyTrip()
        assertNull(destinations.seam)
        destinations.signIn()
        assertNull(destinations.seam)
    }
}
