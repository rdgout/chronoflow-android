package net.chronoflow.app

import net.chronoflow.core.navigation.RootNavigator
import net.chronoflow.core.navigation.Route

interface WelcomeDestinations {
    fun planMyTrip()

    fun signIn()
}

class RoutedWelcomeDestinations(
    private val navigator: RootNavigator,
) : WelcomeDestinations {
    override fun planMyTrip() {
        navigator.navigate(Route.GuestWizard)
    }

    override fun signIn() {
        navigator.navigate(Route.SignIn)
    }
}
