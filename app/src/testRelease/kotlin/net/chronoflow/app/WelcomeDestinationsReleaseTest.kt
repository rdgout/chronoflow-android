package net.chronoflow.app

import net.chronoflow.core.navigation.RootNavigator
import net.chronoflow.core.navigation.Route
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WelcomeDestinationsReleaseTest {
    @Test fun releaseWelcomeActionsNavigate() {
        val navigator = RootNavigator()
        navigator.navigate(Route.Welcome)
        val destinations = LaunchPlatform.welcomeDestinations(navigator)
        assertTrue(destinations is RoutedWelcomeDestinations)
        destinations.planMyTrip()
        assertEquals(Route.GuestWizard, navigator.state.value.route)
        destinations.signIn()
        assertEquals(Route.SignIn, navigator.state.value.route)
    }
}
