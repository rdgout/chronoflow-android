package net.chronoflow.debug

import net.chronoflow.core.navigation.Route
import net.chronoflow.core.navigation.shell.ShellStart
import net.chronoflow.core.navigation.shell.ShellTab
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsShellStartTest {
    private fun seed(
        raw: String,
        plan: String? = null,
    ) {
        DebugShellStart.reset()
        DebugSeedRouting.current =
            TestSeed.parse(
                mapOf(
                    TestSeed.TEST_ID_KEY to "0199a3c0-0000-7000-8000-000000000000",
                    "CF_SETTINGS_ROUTE" to raw,
                ) + if (plan == null) emptyMap() else mapOf("CF_PLAN_TEST_ROUTE" to plan),
            )
    }

    @After fun clear() {
        DebugSeedRouting.current = null
        DebugShellStart.reset()
    }

    @Test fun languageSeedIncludesAccountAndIsConsumedOnce() {
        seed("language")
        assertEquals(ShellStart.Settings(listOf(Route.Account, Route.Language)), DebugShellStart.take())
        assertNull(DebugShellStart.take())
    }

    @Test fun acceptedUnmappedSeedSelectsSettingsRoot() {
        seed("passkeys")
        assertEquals(ShellStart.Settings(emptyList()), DebugShellStart.take())
    }

    @Test fun planSeedKeepsPriority() {
        seed("language", "trips")
        assertEquals(ShellStart.Tab(ShellTab.Trips), DebugShellStart.take())
    }

    @Test fun noAuthTestIdIgnoresTheSetting() {
        DebugShellStart.reset()
        DebugSeedRouting.current = TestSeed.parse(mapOf("CF_SETTINGS_ROUTE" to "language"))
        assertNull(DebugShellStart.take())
    }
}
