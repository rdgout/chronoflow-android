package net.chronoflow.app.settings

import net.chronoflow.core.navigation.Route
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ShellNavSettingsSeedTest(
    private val raw: String?,
    private val expected: List<Route>?,
) {
    @Test fun everyMappingMatchesThePinnedIosPath() {
        assertEquals(expected, SettingsSeedRoute.path(raw))
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "route={0}")
        fun cases(): List<Array<Any?>> =
            listOf(
                arrayOf("profile", listOf(Route.Account, Route.Profile)),
                arrayOf("language", listOf(Route.Account, Route.Language)),
                arrayOf("delete", listOf(Route.Account)),
                arrayOf("password", listOf(Route.Security, Route.Password)),
                arrayOf("twoFactor", listOf(Route.Security, Route.TwoFactorSettings)),
                arrayOf("twoFactorSetup", listOf(Route.Security, Route.TwoFactorSettings)),
                arrayOf("account", listOf(Route.Account)),
                arrayOf("security", listOf(Route.Security)),
                arrayOf("bodyClock", listOf(Route.BodyClock)),
                arrayOf("preferences", listOf(Route.Preferences)),
                arrayOf("plan", listOf(Route.Plan)),
                arrayOf("reminders", listOf(Route.Reminders)),
                arrayOf("twoFactorSettings", null),
                arrayOf("passkeys", null),
                arrayOf("unknown", null),
                arrayOf(null, null),
            )
    }
}
