package net.chronoflow.app.audit

import net.chronoflow.core.testing.tiers.Smoke
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@Smoke
@RunWith(Parameterized::class)
class AuthAuditTest(
    variant: AuditVariant,
) : AuditScreenTest(variant) {
    @Test fun testSignInAudit() = auth("sign-in", "form", "sign-in.email")

    @Test fun testTwoFactorCodeAudit() = auth("two-factor", "form", "two-factor.code")

    @Test fun testTwoFactorRecoveryAudit() = auth("two-factor", "recovery", "two-factor.recovery_code")

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun variants() = auditVariants()
    }
}
