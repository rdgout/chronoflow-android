package net.chronoflow.app.audit

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.testing.tiers.Smoke
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@Smoke
@OptIn(ExperimentalTestApi::class)
@RunWith(Parameterized::class)
class PlanAuditTest(
    variant: AuditVariant,
) : AuditScreenTest(variant) {
    private val account =
        mapOf(
            "CF_AUTH_TEST_SIGNED_IN" to "demo",
            "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
            "CF_PLAN_TEST_FREEZE_CLOCK" to "1",
        )
    private val trip = account + ("CF_PLAN_TEST_ROUTE" to "trip:0199a3c0-0000-7000-8000-000000000001")

    @Test fun testTodayPrimaryState() =
        audit("today.screen", account) {
            compose.waitUntilExactlyOneExists(hasTestTag("today.progress"), 5_000)
            compose.onNodeWithTag("today.progress").assertExists()
        }

    @Test fun testTripsPrimaryState() =
        audit("trips.screen", account + ("CF_PLAN_TEST_ROUTE" to "trips")) {
            compose.waitUntilExactlyOneExists(hasTestTag("trip.0199a3c0-0000-7000-8000-000000000001"), 5_000)
            compose.onNodeWithTag("trip.0199a3c0-0000-7000-8000-000000000001").assertExists()
        }

    @Test fun testTripsEmptyState() =
        audit(
            "trips.empty",
            account + mapOf("CF_PLAN_TEST_ROUTE" to "trips", "CF_REFRESH_TEST_EMPTY_TRIPS" to "1"),
        ) {
            compose.onNodeWithTag("trips.create-empty").assertHasClickAction()
        }

    @Test fun testStepSheetPrimaryState() =
        audit(
            "step.sheet",
            trip + ("CF_PLAN_TEST_SHEET" to "step:sleep-sleep-29865450"),
        )

    @Test fun testLegendPrimaryState() = audit("legend.sheet", trip + ("CF_PLAN_TEST_SHEET" to "legend"))

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun variants() = auditVariants()
    }
}
