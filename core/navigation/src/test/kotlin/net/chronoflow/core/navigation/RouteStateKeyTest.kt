package net.chronoflow.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test

class RouteStateKeyTest {
    @Test fun tripPageStateKeyExcludesNotificationSecrets() {
        val route =
            Route.TripPage(
                "trip",
                notificationInputHash = "private-input-hash",
                notificationToken = "private-notification-token",
            )
        val key = route.stateKey()
        assertFalse(key.contains("private-input-hash"))
        assertFalse(key.contains("private-notification-token"))
        assertEquals(
            key,
            route.copy(notificationInputHash = "other-hash", notificationToken = "other-token").stateKey(),
        )
    }

    @Test fun differentTripsHaveDifferentStateKeys() {
        val first = Route.TripPage("first-trip")
        assertNotEquals(first.stateKey(), first.copy(tripId = "second-trip").stateKey())
    }

    @Test fun tripLocationAndPresentationKeepTheirIdentity() {
        val route = Route.TripPage("trip", "day", "block", sheet = TripSheet.Step("step"))
        assertNotEquals(route.stateKey(), route.copy(dayId = "other-day").stateKey())
        assertNotEquals(route.stateKey(), route.copy(blockId = "other-block").stateKey())
        assertNotEquals(route.stateKey(), route.copy(sheet = TripSheet.Step("other-step")).stateKey())
        assertNotEquals(route.stateKey(), route.copy(awaitingSync = true).stateKey())
    }

    @Test fun otherDataRoutesKeepTheirNonSecretIdentity() {
        assertNotEquals(Route.Reset("first@example.com").stateKey(), Route.Reset("second@example.com").stateKey())
        assertNotEquals(Route.Walkthrough().stateKey(), Route.Walkthrough(replay = true).stateKey())
        assertNotEquals(Route.Wizard(WizardMode.Create).stateKey(), Route.Wizard(WizardMode.Edit("trip")).stateKey())
        assertNotEquals(
            Route.Wizard(WizardMode.Edit("trip")).stateKey(),
            Route.Wizard(WizardMode.Duplicate("trip")).stateKey(),
        )
        assertNotEquals(
            Route.Wizard(WizardMode.Edit("first")).stateKey(),
            Route.Wizard(WizardMode.Edit("second")).stateKey(),
        )
    }

    @Test fun fieldBoundariesAndNullRemainDistinct() {
        assertNotEquals(Route.TripPage("a:b", "c").stateKey(), Route.TripPage("a", "b:c").stateKey())
        assertNotEquals(Route.TripPage("trip").stateKey(), Route.TripPage("trip", "null").stateKey())
    }
}
