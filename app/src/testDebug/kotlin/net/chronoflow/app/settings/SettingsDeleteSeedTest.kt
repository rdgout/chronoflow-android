package net.chronoflow.app.settings

import net.chronoflow.app.LaunchPlatform
import net.chronoflow.core.navigation.Route
import net.chronoflow.debug.DebugSeedRouting
import net.chronoflow.debug.TestSeed
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsDeleteSeedTest {
    @After fun clear() {
        DebugSeedRouting.current = null
    }

    @Test fun deleteOpensAccountWithTheSheet() {
        DebugSeedRouting.current =
            TestSeed.parse(
                mapOf(
                    TestSeed.TEST_ID_KEY to "0199a3c0-0000-7000-8000-000000000000",
                    "CF_SETTINGS_ROUTE" to "delete",
                ),
            )
        assertEquals(listOf(Route.Account), SettingsSeedRoute.path("delete"))
        assertTrue(LaunchPlatform.deleteSheetSeed)
    }

    @Test fun accountDoesNotOpenTheSheet() {
        DebugSeedRouting.current =
            TestSeed.parse(
                mapOf(
                    TestSeed.TEST_ID_KEY to "0199a3c0-0000-7000-8000-000000000000",
                    "CF_SETTINGS_ROUTE" to "account",
                ),
            )
        assertFalse(LaunchPlatform.deleteSheetSeed)
    }
}
