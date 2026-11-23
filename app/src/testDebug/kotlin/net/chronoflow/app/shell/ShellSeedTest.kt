package net.chronoflow.app.shell

import androidx.lifecycle.ViewModelStore
import net.chronoflow.app.LaunchPlatform
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.navigation.shell.ShellNav
import net.chronoflow.core.navigation.shell.ShellTab
import net.chronoflow.debug.DebugSeedRouting
import net.chronoflow.debug.DebugShellStart
import net.chronoflow.debug.TestSeed
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShellSeedTest {
    private val holder = ShellViewModel(ShellNav.initial())
    private val store = ViewModelStore().apply { put("shell", holder) }

    @After fun close() {
        store.clear()
        DebugSeedRouting.current = null
        DebugShellStart.reset()
    }

    private fun launch(route: String = "trips") {
        DebugShellStart.reset()
        DebugSeedRouting.current =
            TestSeed.parse(
                mapOf(
                    TestSeed.TEST_ID_KEY to "0199a3c0-0000-7000-8000-000000000000",
                    "CF_PLAN_TEST_ROUTE" to route,
                ),
            )
        LaunchPlatform.shellStart()?.let(holder::seed)
    }

    @Test fun tripsSeedSelectsMyTrips() {
        launch()
        assertEquals(ShellTab.Trips, holder.state.selected)
        assertEquals(listOf(Route.Trips), holder.state.stacks.getValue(ShellTab.Trips))
    }

    @Test fun tripSeedPushesOntoToday() {
        val id = "0199a3c0-0000-7000-8000-000000000010"
        launch("trip:$id")
        assertEquals(ShellTab.Today, holder.state.selected)
        assertEquals(listOf(Route.Today, Route.TripPage(id)), holder.state.stacks.getValue(ShellTab.Today))
    }

    @Test fun invalidSeedIsIgnored() {
        launch("invalid")
        assertEquals(ShellNav.initial(), holder.state)
    }

    @Test fun secondCompositionDoesNotReapplyTheSeed() {
        launch()
        holder.select(ShellTab.Today)
        assertNull(LaunchPlatform.shellStart())
        assertEquals(ShellTab.Today, holder.state.selected)
    }
}
