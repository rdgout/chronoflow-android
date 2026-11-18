package net.chronoflow.app

import net.chronoflow.core.navigation.RootNavigator
import net.chronoflow.core.navigation.Route

interface WelcomeDestinations {
    fun planMyTrip()

    fun signIn()

    val seam: String? get() = null

    fun closeSeam() {}
}

class RoutedWelcomeDestinations(
    private val navigator: RootNavigator,
) : WelcomeDestinations {
    override fun planMyTrip() {}

    override fun signIn() {
        navigator.navigate(Route.SignIn)
    }
}
