package net.chronoflow.buildlogic.manifest

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import org.junit.Test

class VerifyReleaseManifestTest {
    private fun check(
        file: String,
        allowed: Set<String> = emptySet(),
    ): List<ManifestViolation> {
        val xml = checkNotNull(javaClass.getResource("/manifests/$file")) { "missing $file" }.readText()
        return ManifestRules.check(xml, "net.chronoflow.app.native", allowed)
    }

    private fun rulesOf(file: String) = check(file).map { it.rule }

    @Test
    fun validManifestHasNoViolation() = assertThat(check("valid.xml")).isEmpty()

    @Test
    fun permissionOutsideTheAllowlistFails() = assertThat(rulesOf("permission.xml")).containsExactly(Rule.PERMISSION)

    @Test
    fun customSchemeFails() = assertThat(rulesOf("custom-scheme.xml")).containsExactly(Rule.CUSTOM_SCHEME)

    @Test
    fun exportedDebugComponentFails() =
        assertThat(rulesOf("exported-debug.xml")).containsExactly(Rule.EXPORTED_COMPONENT)

    @Test
    fun debuggableFails() = assertThat(rulesOf("debuggable.xml")).containsExactly(Rule.DEBUGGABLE_OR_TEST_ONLY)

    @Test
    fun testOnlyFails() = assertThat(rulesOf("test-only.xml")).containsExactly(Rule.DEBUGGABLE_OR_TEST_ONLY)

    @Test
    fun cleartextTrafficFails() = assertThat(rulesOf("cleartext.xml")).containsExactly(Rule.CLEARTEXT)

    @Test
    fun networkSecurityConfigFails() =
        assertThat(rulesOf("network-security-config.xml")).containsExactly(Rule.CLEARTEXT)

    @Test
    fun missingLocaleConfigFails() =
        assertThat(rulesOf("missing-locale-config.xml")).containsExactly(Rule.MISSING_LOCALE_CONFIG)

    @Test
    fun backupFails() = assertThat(rulesOf("backup.xml")).containsExactly(Rule.BACKUP)

    @Test
    fun directBootFails() = assertThat(rulesOf("direct-boot.xml")).containsExactly(Rule.DIRECT_BOOT)

    @Test
    fun allowedExportedComponentPasses() =
        assertThat(check("exported-debug.xml", setOf("net.chronoflow.debug.SeedReceiverActivity"))).isEmpty()

    @Test
    fun unapprovedStartupInitializerFails() =
        assertThat(rulesOf("startup-initializer.xml")).containsExactly(Rule.STARTUP_INITIALIZER)

    @Test fun missingRequiredPermissionFails() =
        assertThat(rulesOf("missing-post-notifications.xml")).containsExactly(Rule.REQUIRED_PERMISSION_MISSING)

    @Test fun missingSdkFails() = assertThat(rulesOf("missing-sdk.xml")).containsExactly(Rule.SDK_LEVELS)

    @Test fun minSdk28Fails() = assertThat(rulesOf("min-sdk-28.xml")).containsExactly(Rule.SDK_LEVELS)

    @Test fun targetSdk37Fails() = assertThat(rulesOf("target-sdk-37.xml")).containsExactly(Rule.SDK_LEVELS)

    @Test fun versionMismatchFails() =
        assertThat(
            rulesOf("version-mismatch.xml"),
        ).containsExactly(Rule.VERSION_MISMATCH)

    @Test fun versionNameMismatchFails() =
        assertThat(rulesOf("version-name-mismatch.xml")).containsExactly(Rule.VERSION_MISMATCH)

    @Test fun unknownExportedReceiverFails() =
        assertThat(rulesOf("exported-unknown-receiver.xml")).containsExactly(Rule.EXPORTED_COMPONENT)

    @Test fun exportedSystemJobServiceFails() =
        assertThat(rulesOf("job-service-exported.xml")).containsExactly(Rule.EXPORTED_COMPONENT)
}
