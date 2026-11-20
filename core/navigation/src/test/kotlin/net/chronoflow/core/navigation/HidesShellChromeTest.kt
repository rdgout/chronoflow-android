package net.chronoflow.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class HidesShellChromeTest(
    private val route: Route,
) {
    @Test fun onlyWalkthroughHidesShellChrome() {
        assertEquals(route is Route.Walkthrough, route.hidesShellChrome())
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun routes() =
            listOf(
                Route.Welcome,
                Route.SignIn,
                Route.TwoFactor,
                Route.Forgot,
                Route.SignUp,
                Route.Verify,
                Route.GuestWizard,
                Route.Terms,
                Route.Today,
                Route.Trips,
                Route.Settings,
                Route.Profile,
                Route.Account,
                Route.Language,
                Route.Security,
                Route.Password,
                Route.TwoFactorSettings,
                Route.Passkeys,
                Route.BodyClock,
                Route.Preferences,
                Route.Reminders,
                Route.Plan,
                Route.Reset("a@example.com"),
                Route.Walkthrough(),
                Route.Walkthrough(true),
                Route.TripPage("trip"),
                Route.Wizard(WizardMode.Create),
            ).map { arrayOf(it) }
    }
}
