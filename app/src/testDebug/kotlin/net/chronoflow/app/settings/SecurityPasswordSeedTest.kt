package net.chronoflow.app.settings

import net.chronoflow.app.LaunchPlatform
import net.chronoflow.debug.DebugSeedRouting
import net.chronoflow.debug.TestSeed
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SecurityPasswordSeedTest {
    @After fun clear() {
        DebugSeedRouting.current = null
    }

    @Test fun onlyTheSetupRouteHasTheDebugPassword() {
        DebugSeedRouting.current =
            TestSeed.parse(
                mapOf(
                    TestSeed.TEST_ID_KEY to "0199a3c0-0000-7000-8000-000000000000",
                    "CF_SETTINGS_ROUTE" to "twoFactorSetup",
                ),
            )
        assertEquals("password", LaunchPlatform.passwordPromptSeed)
    }

    @Test fun manageHasNoSeedPassword() {
        DebugSeedRouting.current =
            TestSeed.parse(
                mapOf(
                    TestSeed.TEST_ID_KEY to "0199a3c0-0000-7000-8000-000000000000",
                    "CF_SETTINGS_ROUTE" to "twoFactor",
                ),
            )
        assertNull(LaunchPlatform.passwordPromptSeed)
    }
}
