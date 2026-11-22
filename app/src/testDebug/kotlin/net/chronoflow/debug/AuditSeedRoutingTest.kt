package net.chronoflow.debug

import net.chronoflow.core.navigation.shell.ShellStart
import org.junit.Assert.assertEquals
import org.junit.Test

class AuditSeedRoutingTest {
    @Test fun settingsRootAuditSeedSelectsTheExistingRoot() {
        val previous = DebugSeedRouting.current
        try {
            DebugSeedRouting.current =
                TestSeed.parse(
                    mapOf(
                        TestSeed.TEST_ID_KEY to "019a562b-8000-7000-8000-000000000014",
                        "CF_SETTINGS_ROUTE" to "none",
                    ),
                )
            DebugShellStart.reset()
            assertEquals(ShellStart.Settings(emptyList()), DebugShellStart.take())
        } finally {
            DebugSeedRouting.current = previous
            DebugShellStart.reset()
        }
    }
}
