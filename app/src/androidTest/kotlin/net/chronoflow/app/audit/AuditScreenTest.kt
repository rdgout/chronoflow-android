package net.chronoflow.app.audit

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.test.platform.app.InstrumentationRegistry
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.debug.AuditSeedStatus
import org.junit.Rule
import org.junit.rules.TestRule

data class AuditVariant(
    val language: String,
    val theme: String,
) {
    override fun toString(): String = "$language-$theme"
}

fun auditVariants(): List<Array<Any>> =
    (
        if (InstrumentationRegistry.getArguments().getString("cf.auditFull") == "1") {
            listOf(
                AuditVariant("en", "light"),
                AuditVariant("nl", "light"),
                AuditVariant("en", "dark"),
                AuditVariant("nl", "dark"),
            )
        } else {
            listOf(AuditVariant("en", "light"))
        }
    ).map { arrayOf(it) }

@OptIn(ExperimentalTestApi::class)
abstract class AuditScreenTest(
    private val variant: AuditVariant,
) : ScreenTest() {
    @get:Rule val auditTimings: TestRule = DeviceCaptureTimingRule("a11y")

    protected fun audit(
        tag: String,
        seeds: Map<String, String> = emptyMap(),
        check: () -> Unit = {},
    ) {
        val scenario =
            launch(
                mapOf(
                    "CF_MOCK_DELAY" to "0",
                    "cf.settings.language" to variant.language,
                    "cf.settings.theme" to variant.theme,
                ) +
                    seeds,
            )
        compose.waitUntil(5_000) { AuditSeedStatus.prepared.get() }
        compose.waitUntilExactlyOneExists(hasTestTag(tag), 5_000)
        check()
        compose.runAccessibilityAudit(scenario, tag)
    }

    protected fun auth(
        screen: String,
        state: String,
        tag: String,
        extra: Map<String, String> = emptyMap(),
    ) = audit(tag, mapOf("CF_AUTH_TEST_AUDIT_SCREEN" to screen, "CF_AUTH_TEST_AUDIT_STATE" to state) + extra)
}
