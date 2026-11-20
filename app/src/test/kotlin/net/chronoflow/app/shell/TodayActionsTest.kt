package net.chronoflow.app.shell

import androidx.lifecycle.ViewModelStore
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.navigation.WizardMode
import net.chronoflow.core.navigation.shell.ShellNav
import net.chronoflow.core.navigation.shell.ShellTab
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class TodayActionsTest {
    private val holder = ShellViewModel(ShellNav.initial())
    private val store = ViewModelStore().apply { put("shell", holder) }
    private val actions = todayActions(holder) {}

    @After fun close() = store.clear()

    @Test fun heroPushesOnTodayAndBackReturns() {
        actions.openTrip("hero")
        assertEquals(ShellTab.Today, holder.state.selected)
        assertEquals(listOf(Route.Today, Route.TripPage("hero")), holder.state.stacks.getValue(ShellTab.Today))
        org.junit.Assert.assertTrue(holder.back())
        assertEquals(listOf(Route.Today), holder.state.stacks.getValue(ShellTab.Today))
    }

    @Test fun settingsSelectsItsTabAndKeepsTheTodayStack() {
        holder.openTrip("today-trip")
        actions.settings()
        assertEquals(ShellTab.Settings, holder.state.selected)
        assertEquals(listOf(Route.Today, Route.TripPage("today-trip")), holder.state.stacks.getValue(ShellTab.Today))
    }

    @Test fun pastTripsSelectsTheMyTripsRoot() {
        holder.select(ShellTab.Trips)
        holder.openTrip("old")
        holder.select(ShellTab.Today)
        holder.openTrip("today-trip")
        actions.pastTrips()
        assertEquals(ShellTab.Trips, holder.state.selected)
        assertEquals(listOf(Route.Trips), holder.state.stacks.getValue(ShellTab.Trips))
        assertEquals(listOf(Route.Today), holder.state.stacks.getValue(ShellTab.Today))
    }

    @Test fun createOpensTheWizardOverlay() {
        actions.create()
        assertEquals(Route.Wizard(WizardMode.Create), holder.state.overlay)
        assertEquals(ShellTab.Today, holder.state.selected)
        assertEquals(listOf(Route.Today), holder.state.stacks.getValue(ShellTab.Today))
    }
}
