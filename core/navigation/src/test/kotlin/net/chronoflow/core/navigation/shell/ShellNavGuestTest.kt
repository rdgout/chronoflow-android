package net.chronoflow.core.navigation.shell

import net.chronoflow.core.navigation.Route
import net.chronoflow.core.navigation.WizardMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShellNavGuestTest {
    @Test fun guestTripsClearsTheOverlayAndSelectsMyTrips() {
        val initial = ShellNav.initial().copy(overlay = Route.Wizard(WizardMode.Create))
        val state = ShellNav.guestTrips(initial)
        assertNull(state.overlay)
        assertEquals(ShellTab.Trips, state.selected)
        assertEquals(listOf(Route.Trips), state.stacks[ShellTab.Trips])
    }

    @Test fun wizardSavedStillOpensTheTrip() {
        val initial = ShellNav.initial().copy(overlay = Route.Wizard(WizardMode.Create))
        val state = ShellNav.wizardSaved(initial, "trip")
        assertNull(state.overlay)
        assertEquals(ShellTab.Trips, state.selected)
        assertEquals(listOf(Route.Trips, Route.TripPage("trip", awaitingSync = true)), state.stacks[ShellTab.Trips])
    }
}
