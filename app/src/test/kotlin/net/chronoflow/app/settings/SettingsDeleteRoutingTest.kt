package net.chronoflow.app.settings

import net.chronoflow.core.navigation.Route
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsDeleteRoutingTest {
    @Test fun lateSignOutKeepsTheDeleteDestination() {
        assertNull(signedOutDestination(Route.Welcome, false))
    }

    @Test fun ordinarySignOutOpensSignIn() {
        assertEquals(Route.SignIn, signedOutDestination(Route.Today, false))
    }

    @Test fun repeatedSignOutKeepsSignIn() {
        assertNull(signedOutDestination(Route.SignIn, false))
    }

    @Test fun deleteSignOutGoesStraightToWelcome() {
        assertEquals(Route.Welcome, signedOutDestination(Route.Today, false, Route.Welcome))
    }

    @Test fun repeatedDeleteSignOutKeepsWelcome() {
        assertNull(signedOutDestination(Route.Welcome, false, Route.Welcome))
    }
}
