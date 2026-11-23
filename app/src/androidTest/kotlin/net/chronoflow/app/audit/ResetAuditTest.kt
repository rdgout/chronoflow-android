package net.chronoflow.app.audit

import net.chronoflow.core.testing.tiers.Audit
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@Audit
@RunWith(Parameterized::class)
class ResetAuditTest(
    variant: AuditVariant,
) : AuditScreenTest(variant) {
    @Test fun testResetFormAudit() = auth("reset", "form", "reset.submit")

    @Test fun testResetPasswordAudit() = auth("reset", "password", "reset.submit")

    @Test fun testResetConfirmationAudit() = auth("reset", "confirmation", "reset.submit")

    @Test fun testResetExpiredAudit() = auth("reset", "expired", "forgot.submit")

    @Test fun testResetDoneAudit() = auth("reset", "done", "reset.continue")

    @Test fun testResetServerAudit() =
        auth(
            "reset",
            "server",
            "auth.banner",
            mapOf("CF_AUTH_TEST_RESET_FAILURE" to "server"),
        )

    @Test fun testResetThrottleAudit() =
        auth(
            "reset",
            "throttle",
            "auth.banner",
            mapOf("CF_AUTH_TEST_RESET_FAILURE" to "throttle"),
        )

    @Test fun testResetOfflineAudit() =
        auth(
            "reset",
            "offline",
            "auth.banner",
            mapOf("CF_AUTH_TEST_RESET_FAILURE" to "offline"),
        )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun variants() = auditVariants()
    }
}
