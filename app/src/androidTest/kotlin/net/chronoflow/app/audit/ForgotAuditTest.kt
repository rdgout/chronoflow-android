package net.chronoflow.app.audit

import net.chronoflow.core.testing.tiers.Audit
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@Audit
@RunWith(Parameterized::class)
class ForgotAuditTest(
    variant: AuditVariant,
) : AuditScreenTest(variant) {
    @Test fun testForgotFormAudit() = auth("forgot", "form", "forgot.submit")

    @Test fun testForgotSentAudit() = auth("forgot", "sent", "forgot.notice")

    @Test fun testForgotFieldAudit() = auth("forgot", "field", "forgot.email")

    @Test fun testForgotServerAudit() = auth("forgot", "server", "auth.banner")

    @Test fun testForgotThrottleAudit() = auth("forgot", "throttle", "auth.banner")

    @Test fun testForgotOfflineAudit() = auth("forgot", "offline", "auth.banner")

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun variants() = auditVariants()
    }
}
