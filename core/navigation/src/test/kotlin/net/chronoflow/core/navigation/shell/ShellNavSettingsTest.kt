package net.chronoflow.core.navigation.shell

import net.chronoflow.core.navigation.Route
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellNavSettingsTest {
    @Test fun settingsSeedSelectsTheTabAndBackPopsProfileThenAccount() {
        val initial = ShellNav.openTrip(ShellNav.initial(ShellTab.Trips), "kept")
        val seeded = ShellNav.seed(initial, ShellStart.Settings(listOf(Route.Account, Route.Profile)))
        assertEquals(ShellTab.Settings, seeded.selected)
        assertEquals(listOf(Route.Settings, Route.Account, Route.Profile), seeded.stacks[ShellTab.Settings])
        assertEquals(initial.stacks[ShellTab.Trips], seeded.stacks[ShellTab.Trips])
        val (account, firstHandled) = ShellNav.back(seeded)
        assertTrue(firstHandled)
        assertEquals(listOf(Route.Settings, Route.Account), account.stacks[ShellTab.Settings])
        val (settings, secondHandled) = ShellNav.back(account)
        assertTrue(secondHandled)
        assertEquals(listOf(Route.Settings), settings.stacks[ShellTab.Settings])
        assertEquals(ShellTab.Settings, settings.selected)
    }

    @Test fun emptyPathSelectsSettingsRoot() {
        val state = ShellNav.seed(ShellNav.initial(), ShellStart.Settings(emptyList()))
        assertEquals(ShellTab.Settings, state.selected)
        assertEquals(listOf(Route.Settings), state.stacks[ShellTab.Settings])
    }
}
