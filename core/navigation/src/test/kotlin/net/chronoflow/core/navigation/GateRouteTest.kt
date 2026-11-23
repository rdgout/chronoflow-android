package net.chronoflow.core.navigation

import net.chronoflow.core.model.launch.Gate
import org.junit.Assert.assertEquals
import org.junit.Test

class GateRouteTest {
    @Test fun verifyRoutesToVerify() = assertEquals(Route.Verify, Gate.Verify.toRoute())

    @Test fun termsRoutesToTerms() = assertEquals(Route.Terms, Gate.Terms.toRoute())

    @Test fun walkthroughRoutesToFirstRunWalkthrough() =
        assertEquals(
            Route.Walkthrough(false),
            Gate.Walkthrough.toRoute(),
        )

    @Test fun appRoutesToToday() = assertEquals(Route.Today, Gate.App.toRoute())
}
