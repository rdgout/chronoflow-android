package net.chronoflow.app

import net.chronoflow.core.navigation.RootNavigator
import net.chronoflow.core.navigation.Route
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WelcomeDestinationsReleaseTest {
    @Test fun releaseWelcomeActionsRemainInert() {
        val navigator = RootNavigator()
        navigator.navigate(Route.Welcome)
        val destinations = LaunchPlatform.welcomeDestinations(navigator)
        assertTrue(destinations is RoutedWelcomeDestinations)
        assertNull(destinations.seam)
        val revision = navigator.state.value.revision
        destinations.planMyTrip()
        assertEquals(revision, navigator.state.value.revision)
        destinations.signIn()
        assertEquals(Route.SignIn, navigator.state.value.route)
        assertNull(destinations.seam)
    }
}
