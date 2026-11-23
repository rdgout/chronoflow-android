package net.chronoflow.debug

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import net.chronoflow.app.WelcomeDestinations
import net.chronoflow.core.navigation.RootNavigator
import net.chronoflow.core.navigation.Route

class DebugWelcomeDestinations(
    private val navigator: RootNavigator,
) : WelcomeDestinations {
    override var seam by mutableStateOf<String?>(null)
        private set

    override fun planMyTrip() {
        seam = "guestWizard"
    }

    override fun signIn() {
        navigator.navigate(Route.SignIn)
    }

    override fun closeSeam() {
        seam = null
    }
}
