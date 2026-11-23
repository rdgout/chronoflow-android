package net.chronoflow.app.wizard

import net.chronoflow.core.navigation.Route
import net.chronoflow.core.navigation.WizardMode
import org.junit.Assert.assertEquals
import org.junit.Test

class WizardLauncherTest {
    @Test fun twoPendingCallsPushOnce() {
        val pushes = mutableListOf<Route>()
        val launcher = WizardLauncher({ null }, pushes::add)
        launcher.open()
        launcher.open()
        assertEquals(listOf(Route.Wizard(WizardMode.Create)), pushes)
    }

    @Test fun anExistingWizardDoesNotOpenAgain() {
        val pushes = mutableListOf<Route>()
        val launcher = WizardLauncher({ Route.Wizard(WizardMode.Create) }, pushes::add)
        launcher.open()
        assertEquals(emptyList<Route>(), pushes)
    }

    @Test fun closingAnOverlayAllowsAnotherOpen() {
        var overlay: Route? = null
        val pushes = mutableListOf<Route>()
        val launcher =
            WizardLauncher({ overlay }) {
                pushes.add(it)
                overlay = it
            }
        launcher.open()
        launcher.overlayChanged()
        overlay = null
        launcher.overlayChanged()
        launcher.open()
        assertEquals(2, pushes.size)
    }
}
