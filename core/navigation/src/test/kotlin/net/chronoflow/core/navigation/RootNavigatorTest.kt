package net.chronoflow.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class RootNavigatorTest {
    @Test fun revisionsIncreaseForEveryNavigationIncludingRepeatedRoutes() {
        val navigator = RootNavigator()
        assertEquals(RootState(null, 0), navigator.state.value)
        navigator.navigate(Route.Terms)
        assertEquals(RootState(Route.Terms, 1), navigator.state.value)
        navigator.navigate(Route.Terms)
        assertEquals(RootState(Route.Terms, 2), navigator.state.value)
    }
}
