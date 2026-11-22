package net.chronoflow.app.audit

import net.chronoflow.core.testing.tiers.Audit
import org.junit.Test

@Audit
class DayNavigationAccessibilityTest : AuditScreenTest(AuditVariant("en", "light")) {
    @Test fun summaryPreviousDayKeepsItsFullAccessibilityTarget() =
        audit(
            "trip.summary",
            mapOf(
                "CF_AUTH_TEST_SIGNED_IN" to "demo",
                "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
                "CF_PLAN_TEST_ROUTE" to "trip:0199a3c0-0000-7000-8000-000000000001",
                "CF_PLAN_TEST_POSITION" to "summary",
                "CF_PLAN_TEST_FREEZE_CLOCK" to "1",
            ),
        )
}
