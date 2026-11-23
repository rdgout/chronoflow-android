package net.chronoflow.debug

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import net.chronoflow.app.WelcomeDestinations

class DebugWelcomeDestinations : WelcomeDestinations {
    override var seam by mutableStateOf<String?>(null)
        private set

    override fun planMyTrip() {
        seam = "guestWizard"
    }

    override fun signIn() {
        seam = "signIn"
    }

    override fun closeSeam() {
        seam = null
    }
}
