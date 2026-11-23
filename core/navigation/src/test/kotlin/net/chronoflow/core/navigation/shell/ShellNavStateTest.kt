package net.chronoflow.core.navigation.shell

import net.chronoflow.core.navigation.Route
import net.chronoflow.core.navigation.WizardMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellNavStateTest {
    @Test fun initialCreatesThreeRootStacks() {
        val state = ShellNav.initial(ShellTab.Trips)
        assertEquals(ShellTab.Trips, state.selected)
        assertEquals(
            mapOf(
                ShellTab.Today to listOf(Route.Today),
                ShellTab.Trips to listOf(Route.Trips),
                ShellTab.Settings to listOf(Route.Settings),
            ),
            state.stacks,
        )
    }

    @Test fun selectingAnotherTabKeepsEveryStack() {
        val state = ShellNav.openTrip(ShellNav.initial(), "t1")
        val selected = ShellNav.select(state, ShellTab.Settings)
        assertEquals(ShellTab.Settings, selected.selected)
        assertEquals(state.stacks, selected.stacks)
    }

    @Test fun reselectPopsToRootWithoutScrolling() {
        val state = ShellNav.openTrip(ShellNav.initial(ShellTab.Trips), "t1")
        val next = ShellNav.select(state, ShellTab.Trips)
        assertEquals(listOf(Route.Trips), next.stacks[ShellTab.Trips])
        assertEquals(state.scrollToTop, next.scrollToTop)
    }

    @Test fun reselectAtRootIncrementsOnlyThatTabsSignal() {
        val state = ShellNav.initial(ShellTab.Trips).copy(scrollToTop = mapOf(ShellTab.Today to 2))
        assertEquals(
            mapOf(ShellTab.Today to 2, ShellTab.Trips to 1),
            ShellNav.select(state, ShellTab.Trips).scrollToTop,
        )
    }

    @Test fun pushAppendsToSelectedStack() {
        val state = ShellNav.push(ShellNav.initial(ShellTab.Settings), Route.Profile)
        assertEquals(listOf(Route.Settings, Route.Profile), state.stacks[ShellTab.Settings])
    }

    @Test fun wizardPushOpensAnOverlay() {
        val wizard = Route.Wizard(WizardMode.Create)
        val initial = ShellNav.initial()
        val next = ShellNav.push(initial, wizard)
        assertEquals(wizard, next.overlay)
        assertEquals(initial.stacks, next.stacks)
    }

    @Test fun openTripUsesTheSelectedStack() {
        val state = ShellNav.openTrip(ShellNav.initial(), "t1")
        assertEquals(listOf(Route.Today, Route.TripPage("t1")), state.stacks[ShellTab.Today])
    }

    @Test fun backClosesOverlayBeforePopping() {
        val page = ShellNav.openTrip(ShellNav.initial(), "t1")
        val (state, handled) = ShellNav.back(ShellNav.push(page, Route.Wizard(WizardMode.Create)))
        assertTrue(handled)
        assertNull(state.overlay)
        assertEquals(page.stacks, state.stacks)
    }

    @Test fun backPopsTheSelectedStack() {
        val (state, handled) = ShellNav.back(ShellNav.openTrip(ShellNav.initial(), "t1"))
        assertTrue(handled)
        assertEquals(ShellNav.initial(), state)
    }

    @Test fun backFromNonTodayRootSelectsTodayThenIsUnhandled() {
        val (state, handled) = ShellNav.back(ShellNav.initial(ShellTab.Trips))
        assertTrue(handled)
        assertEquals(ShellTab.Today, state.selected)
        assertFalse(ShellNav.back(state).second)
    }

    @Test fun wizardSavedOpensTheTripsStackAwaitingSync() {
        val state = ShellNav.wizardSaved(ShellNav.push(ShellNav.initial(), Route.Wizard(WizardMode.Create)), "saved")
        assertNull(state.overlay)
        assertEquals(ShellTab.Trips, state.selected)
        assertEquals(listOf(Route.Trips, Route.TripPage("saved", awaitingSync = true)), state.stacks[ShellTab.Trips])
    }

    @Test fun fromRootMapsEachShellRoute() {
        assertEquals(ShellNav.initial(), ShellNav.fromRoot(Route.Today))
        assertEquals(ShellNav.initial(ShellTab.Trips), ShellNav.fromRoot(Route.Trips))
        assertEquals(ShellNav.initial(ShellTab.Settings), ShellNav.fromRoot(Route.Settings))
        val route = Route.TripPage("t1")
        assertEquals(ShellNav.push(ShellNav.initial(ShellTab.Trips), route), ShellNav.fromRoot(route))
        val wizard = Route.Wizard(WizardMode.Create)
        assertEquals(ShellNav.push(ShellNav.initial(ShellTab.Trips), wizard), ShellNav.fromRoot(wizard))
    }

    @Test fun fromRootRejectsAuthRoutes() {
        assertThrows(IllegalArgumentException::class.java) { ShellNav.fromRoot(Route.SignIn) }
    }

    @Test fun seedSelectsATab() {
        assertEquals(ShellTab.Trips, ShellNav.seed(ShellNav.initial(), ShellStart.Tab(ShellTab.Trips)).selected)
    }

    @Test fun seedTripUsesTodayWhileKeepingOtherStacks() {
        val initial = ShellNav.openTrip(ShellNav.initial(ShellTab.Trips), "kept")
        val seeded = ShellNav.seed(initial, ShellStart.Trip("seed"))
        assertEquals(ShellTab.Today, seeded.selected)
        assertEquals(listOf(Route.Today, Route.TripPage("seed")), seeded.stacks[ShellTab.Today])
        assertEquals(initial.stacks[ShellTab.Trips], seeded.stacks[ShellTab.Trips])
    }

    @Test fun routesHoldNoSecret() {
        assertEquals(
            "TripPage(tripId=t1, dayId=null, blockId=null, awaitingSync=false, notificationInputHash=null, notificationToken=null, sheet=null)",
            Route.TripPage("t1").toString(),
        )
    }
}
