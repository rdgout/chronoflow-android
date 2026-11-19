package net.chronoflow.core.navigation

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class RouteSerializationTest(
    private val name: String,
    private val route: Route,
) {
    @Test fun routesRoundTripThroughJson() {
        assertEquals(route, Json.decodeFromString(Route.serializer(), Json.encodeToString(Route.serializer(), route)))
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}")
        fun cases() = routeCases()
    }
}
