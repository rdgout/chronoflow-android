package net.chronoflow.app.audit

import net.chronoflow.core.testing.tiers.Audit
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@Audit
@RunWith(Parameterized::class)
class SignUpAuditTest(
    variant: AuditVariant,
) : AuditScreenTest(variant) {
    @Test fun testSignUpFormAudit() = auth("sign-up", "form", "sign-up.submit")

    @Ignore("no seed: entry-scoped SignUpModel prefill/failure, OQ-14-6")
    @Test
    fun testSignUpEmptyAudit() = auth("sign-up", "empty", "sign-up.submit")

    @Ignore("no seed: entry-scoped SignUpModel prefill/failure, OQ-14-6")
    @Test
    fun testSignUpEmailAudit() = auth("sign-up", "email", "sign-up.submit")

    @Ignore("no seed: entry-scoped SignUpModel prefill/failure, OQ-14-6")
    @Test
    fun testSignUpTakenAudit() = auth("sign-up", "taken", "auth.banner")

    @Ignore("no seed: entry-scoped SignUpModel prefill/failure, OQ-14-6")
    @Test
    fun testSignUpPasswordAudit() = auth("sign-up", "password", "sign-up.submit")

    @Ignore("no seed: entry-scoped SignUpModel prefill/failure, OQ-14-6")
    @Test
    fun testSignUpConfirmationAudit() = auth("sign-up", "confirmation", "sign-up.submit")

    @Ignore("no seed: entry-scoped SignUpModel prefill/failure, OQ-14-6")
    @Test
    fun testSignUpVisibleAudit() = auth("sign-up", "visible", "sign-up.visibility")

    @Ignore("no seed: entry-scoped SignUpModel prefill/failure, OQ-14-6")
    @Test
    fun testSignUpServerAudit() =
        auth(
            "sign-up",
            "server",
            "auth.banner",
            mapOf("CF_AUTH_TEST_SIGNUP_FAILURE" to "server"),
        )

    @Ignore("no seed: entry-scoped SignUpModel prefill/failure, OQ-14-6")
    @Test
    fun testSignUpThrottleAudit() =
        auth(
            "sign-up",
            "throttle",
            "auth.banner",
            mapOf("CF_AUTH_TEST_SIGNUP_FAILURE" to "throttle"),
        )

    @Ignore("no seed: entry-scoped SignUpModel prefill/failure, OQ-14-6")
    @Test
    fun testSignUpOfflineAudit() =
        auth(
            "sign-up",
            "offline",
            "auth.banner",
            mapOf("CF_AUTH_TEST_SIGNUP_FAILURE" to "offline"),
        )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun variants() = auditVariants()
    }
}
