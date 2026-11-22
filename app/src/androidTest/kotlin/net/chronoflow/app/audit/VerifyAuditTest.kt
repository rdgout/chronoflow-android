package net.chronoflow.app.audit

import net.chronoflow.core.testing.tiers.Audit
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@Audit
@RunWith(Parameterized::class)
class VerifyAuditTest(
    variant: AuditVariant,
) : AuditScreenTest(variant) {
    @Test fun testVerifyWaitingAudit() =
        auth(
            "verify",
            "waiting",
            "verify.open-email",
            mapOf(
                "CF_AUTH_TEST_SIGNED_IN" to "demo",
                "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
                "CF_AUTH_TEST_UNVERIFIED" to "1",
                "CF_AUTH_TEST_VERIFY_STATE" to "waiting",
            ),
        )

    @Test fun testVerifyCountdownAudit() =
        auth(
            "verify",
            "countdown",
            "verify.countdown",
            mapOf(
                "CF_AUTH_TEST_SIGNED_IN" to "demo",
                "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
                "CF_AUTH_TEST_UNVERIFIED" to "1",
                "CF_AUTH_TEST_VERIFY_STATE" to "countdown",
            ),
        )

    @Test fun testVerifyFailedAudit() =
        auth(
            "verify",
            "failed",
            "verify.retry",
            mapOf(
                "CF_AUTH_TEST_SIGNED_IN" to "demo",
                "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
                "CF_AUTH_TEST_UNVERIFIED" to "1",
                "CF_AUTH_TEST_VERIFY_STATE" to "failed",
            ),
        )

    @Test fun testVerifyServerAudit() =
        auth(
            "verify",
            "server",
            "auth.banner",
            mapOf(
                "CF_AUTH_TEST_SIGNED_IN" to "demo",
                "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
                "CF_AUTH_TEST_UNVERIFIED" to "1",
                "CF_AUTH_TEST_VERIFY_STATE" to "server",
            ),
        )

    @Test fun testVerifyThrottleAudit() =
        auth(
            "verify",
            "throttle",
            "verify.countdown",
            mapOf(
                "CF_AUTH_TEST_SIGNED_IN" to "demo",
                "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
                "CF_AUTH_TEST_UNVERIFIED" to "1",
                "CF_AUTH_TEST_VERIFY_STATE" to "throttle",
            ),
        )

    @Test fun testVerifyOfflineAudit() =
        auth(
            "verify",
            "offline",
            "auth.banner",
            mapOf(
                "CF_AUTH_TEST_SIGNED_IN" to "demo",
                "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
                "CF_AUTH_TEST_UNVERIFIED" to "1",
                "CF_AUTH_TEST_VERIFY_STATE" to "offline",
            ),
        )

    @Ignore("no seed: verification sign-out failure, OQ-14-6")
    @Test
    fun testVerifySignOutErrorAudit() =
        auth(
            "verify",
            "signOutError",
            "verify.sign-out-error",
            mapOf(
                "CF_AUTH_TEST_SIGNED_IN" to "demo",
                "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
                "CF_AUTH_TEST_UNVERIFIED" to "1",
                "CF_AUTH_TEST_VERIFY_STATE" to "signOutError",
            ),
        )

    @Test fun testVerifyFailedServerAudit() =
        auth(
            "verify",
            "failedServer",
            "auth.banner",
            mapOf(
                "CF_AUTH_TEST_SIGNED_IN" to "demo",
                "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
                "CF_AUTH_TEST_UNVERIFIED" to "1",
                "CF_AUTH_TEST_VERIFY_STATE" to "failedServer",
            ),
        )

    @Test fun testVerifyFailedThrottleAudit() =
        auth(
            "verify",
            "failedThrottle",
            "auth.banner",
            mapOf(
                "CF_AUTH_TEST_SIGNED_IN" to "demo",
                "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
                "CF_AUTH_TEST_UNVERIFIED" to "1",
                "CF_AUTH_TEST_VERIFY_STATE" to "failedThrottle",
            ),
        )

    @Test fun testVerifyFailedOfflineAudit() =
        auth(
            "verify",
            "failedOffline",
            "auth.banner",
            mapOf(
                "CF_AUTH_TEST_SIGNED_IN" to "demo",
                "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
                "CF_AUTH_TEST_UNVERIFIED" to "1",
                "CF_AUTH_TEST_VERIFY_STATE" to "failedOffline",
            ),
        )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun variants() = auditVariants()
    }
}
