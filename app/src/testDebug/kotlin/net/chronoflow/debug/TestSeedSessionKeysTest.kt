@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.debug

import net.chronoflow.core.navigation.TripSheet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class TestSeedSessionKeysTest(
    private val row: Row,
) {
    data class Row(
        val name: String,
        val key: String,
        val value: String,
        val gated: Boolean,
        val expected: Any?,
        val ignored: Boolean,
    ) {
        override fun toString() = name
    }

    @Test fun sessionKeysFollowTheirGrammarAndRequireAnAuthTestId() {
        val seed =
            TestSeed.parse(
                mapOf(row.key to row.value) + if (row.gated) mapOf(TestSeed.TEST_ID_KEY to TEST_ID) else emptyMap(),
            )
        val actual: Any? =
            when (row.key) {
                "CF_AUTH_TEST_SIGNED_IN" -> seed.signedIn
                "CF_AUTH_TEST_WALKTHROUGH_SEEN" -> seed.walkthroughSeen
                "CF_AUTH_TEST_TERMS_PENDING" -> seed.termsPending
                "CF_AUTH_TEST_UNVERIFIED" -> seed.unverified
                "CF_AUTH_TEST_AUDIT_SCREEN" -> seed.auditScreen
                "CF_AUTH_TEST_AUDIT_STATE" -> seed.auditState
                "CF_MOCK_DELAY" -> seed.mockDelay
                "CF_PLAN_TEST_ROUTE" -> seed.planRoute
                "CF_PLAN_TEST_POSITION" -> seed.planSummary
                "CF_PLAN_TEST_SHEET" -> seed.planSheet
                "CF_SETTINGS_ROUTE" -> seed.settingsRoute
                "CF_WIZARD_AUTO_OPEN" -> seed.wizardAutoOpen
                "CF_WIZARD_BOARD_CAPTURE" -> seed.wizardBoardCapture
                else -> error("unlisted seed key")
            }
        assertEquals(row.expected, actual)
        assertEquals(row.ignored, row.key in seed.ignoredKeys)
        if (!row.gated) assertFalse(row.key in seed.raw)
        assertTrue(seed.toString().contains("keys="))
    }

    companion object {
        private const val TEST_ID = "019a562b-8000-7000-8000-000000000001"

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> {
            val valid =
                listOf(
                    Row("account", "CF_AUTH_TEST_SIGNED_IN", "demo", true, SeedAccount.Demo, false),
                    Row("walkthrough", "CF_AUTH_TEST_WALKTHROUGH_SEEN", "1", true, true, false),
                    Row("terms", "CF_AUTH_TEST_TERMS_PENDING", "1", true, true, false),
                    Row("verify", "CF_AUTH_TEST_UNVERIFIED", "1", true, true, false),
                    Row("audit-screen", "CF_AUTH_TEST_AUDIT_SCREEN", "sign-in", true, "sign-in", false),
                    Row("audit-state", "CF_AUTH_TEST_AUDIT_STATE", "failed", true, "failed", false),
                    Row("delay", "CF_MOCK_DELAY", "1.5", true, 1.5.seconds, false),
                    Row(
                        "trip-route",
                        "CF_PLAN_TEST_ROUTE",
                        "trip:$TEST_ID:list",
                        true,
                        PlanRouteSeed.Trip(TEST_ID, true),
                        false,
                    ),
                    Row("summary", "CF_PLAN_TEST_POSITION", "summary", true, true, false),
                    Row("step", "CF_PLAN_TEST_SHEET", "step:block", true, TripSheet.Step("block"), false),
                    Row("settings", "CF_SETTINGS_ROUTE", "twoFactorSetup", true, "twoFactorSetup", false),
                    Row("wizard", "CF_WIZARD_AUTO_OPEN", "1", true, true, false),
                    Row("board", "CF_WIZARD_BOARD_CAPTURE", "WizardWhere", true, "WizardWhere", false),
                )

            fun empty(key: String): Any? =
                when (key) {
                    "CF_AUTH_TEST_WALKTHROUGH_SEEN", "CF_AUTH_TEST_TERMS_PENDING", "CF_AUTH_TEST_UNVERIFIED", "CF_PLAN_TEST_POSITION", "CF_WIZARD_AUTO_OPEN" -> false
                    "CF_MOCK_DELAY" -> Duration.ZERO
                    else -> null
                }
            val ungated =
                valid.map {
                    it.copy(name = it.name + "-ungated", gated = false, expected = empty(it.key), ignored = true)
                }
            val invalid =
                valid
                    .filter {
                        it.key !in
                            setOf("CF_AUTH_TEST_AUDIT_SCREEN", "CF_AUTH_TEST_AUDIT_STATE", "CF_WIZARD_BOARD_CAPTURE")
                    }.map {
                        it.copy(
                            name = it.name + "-invalid",
                            value = "invalid",
                            expected = empty(it.key),
                            ignored = true,
                        )
                    }
            val delays =
                listOf("0", "60", "60.1", "-1", "NaN", "abc").map { value ->
                    val validDelay = value.toDoubleOrNull()?.takeIf { it.isFinite() && it in 0.0..60.0 }
                    Row(
                        "delay-$value",
                        "CF_MOCK_DELAY",
                        value,
                        true,
                        validDelay?.seconds ?: Duration.ZERO,
                        validDelay == null,
                    )
                }
            val extra =
                listOf(
                    Row("twofactor", "CF_AUTH_TEST_SIGNED_IN", "twofactor", true, SeedAccount.TwoFactor, false),
                    Row("trips", "CF_PLAN_TEST_ROUTE", "trips", true, PlanRouteSeed.Trips, false),
                    Row("trip", "CF_PLAN_TEST_ROUTE", "trip:$TEST_ID", true, PlanRouteSeed.Trip(TEST_ID), false),
                    Row("legend", "CF_PLAN_TEST_SHEET", "legend", true, TripSheet.Legend, false),
                    Row("empty-step", "CF_PLAN_TEST_SHEET", "step:", true, null, true),
                    Row("raw-audit-screen", "CF_AUTH_TEST_AUDIT_SCREEN", "future-screen", true, "future-screen", false),
                    Row("raw-audit-state", "CF_AUTH_TEST_AUDIT_STATE", "", true, "", false),
                    Row("raw-board", "CF_WIZARD_BOARD_CAPTURE", "future-board", true, "future-board", false),
                )
            return (valid + ungated + invalid + delays + extra).map { arrayOf<Any>(it) }
        }
    }
}
