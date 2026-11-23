package net.chronoflow.app.audit

import net.chronoflow.core.model.passkey.PasskeyCapability
import net.chronoflow.core.testing.audit.AuditScreen
import net.chronoflow.core.testing.audit.auditScreens
import net.chronoflow.core.testing.tiers.Audit
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@Audit
@RunWith(Parameterized::class)
class ScreenAccessibilityAuditTest(
    private val screen: AuditScreen,
    variant: AuditVariant,
) : AuditScreenTest(variant) {
    @Test fun seededScreenPassesTheAccessibilityPolicy() {
        assumeTrue(
            "passkey capability disabled: OQ-A12/OQ-A21",
            screen.name != "SetPasskeys" || PasskeyCapability.SERVER_READY,
        )
        audit(screen.contentTag, screen.seeds)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}-{1}")
        fun cases(): List<Array<Any>> =
            auditScreens.flatMap { screen ->
                auditVariants().map { variant -> arrayOf(screen, variant.single()) }
            }
    }
}
