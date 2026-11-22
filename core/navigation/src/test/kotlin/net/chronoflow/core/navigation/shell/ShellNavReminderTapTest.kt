package net.chronoflow.core.navigation.shell

import net.chronoflow.core.navigation.ReminderTapExtras
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.navigation.WizardMode
import org.junit.Assert.assertEquals
import org.junit.Test

class ShellNavReminderTapTest {
    @Test fun tapReplacesOnlyTripsStackAndKeepsWizardOverlay() {
        val state = ShellNav.push(ShellNav.openTrip(ShellNav.initial(), "x"), Route.Wizard(WizardMode.Create))
        val next = ShellNav.reminderTap(state, ReminderTapExtras("trip-id", "light", "account", "hash"), "tok")
        assertEquals(ShellTab.Trips, next.selected)
        assertEquals(
            listOf(
                Route.Trips,
                Route.TripPage("trip-id", blockId = "light", notificationInputHash = "hash", notificationToken = "tok"),
            ),
            next.stacks[ShellTab.Trips],
        )
        assertEquals(state.stacks[ShellTab.Today], next.stacks[ShellTab.Today])
        assertEquals(state.stacks[ShellTab.Settings], next.stacks[ShellTab.Settings])
        assertEquals(state.overlay, next.overlay)
    }
}
