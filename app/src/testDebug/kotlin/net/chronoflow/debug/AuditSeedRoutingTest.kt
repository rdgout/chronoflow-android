package net.chronoflow.debug

import net.chronoflow.core.navigation.Route
import net.chronoflow.core.navigation.shell.ShellStart
import org.junit.Assert.assertEquals
import org.junit.Test

class AuditSeedRoutingTest {
    @Test fun auditPasskeysAliasOpensTheExistingLeaf() =
        assertAuditRoute(
            "passkeys",
            listOf(Route.Security, Route.Passkeys),
        )

    @Test fun auditTwoFactorSettingsAliasOpensTheExistingLeaf() =
        assertAuditRoute(
            "twoFactorSettings",
            listOf(Route.Security, Route.TwoFactorSettings),
        )

    @Test fun legacyUnmappedPasskeysSeedStillSelectsSettingsRoot() =
        assertAuditRoute(
            "passkeys",
            emptyList(),
            audit = false,
        )

    private fun assertAuditRoute(
        raw: String,
        expected: List<Route>,
        audit: Boolean = true,
    ) {
        val previous = DebugSeedRouting.current
        try {
            DebugSeedRouting.current =
                TestSeed.parse(
                    mapOf(
                        TestSeed.TEST_ID_KEY to "019a562b-8000-7000-8000-000000000014",
                        "CF_SETTINGS_ROUTE" to raw,
                    ) + if (audit) mapOf("CF_AUTH_TEST_AUDIT_SCREEN" to "settings") else emptyMap(),
                )
            DebugShellStart.reset()
            assertEquals(ShellStart.Settings(expected), DebugShellStart.take())
        } finally {
            DebugSeedRouting.current = previous
            DebugShellStart.reset()
        }
    }

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
