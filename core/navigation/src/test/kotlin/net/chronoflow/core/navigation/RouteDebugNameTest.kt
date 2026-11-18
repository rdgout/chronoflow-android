package net.chronoflow.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class RouteDebugNameTest(
    private val name: String,
    private val route: Route,
) {
    @Test fun debugNameMatchesTheRoute() {
        assertEquals(name, route.debugName())
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}")
        fun cases() = routeCases()
    }
}
