package net.chronoflow.debug

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.chronoflow.core.testing.audit.auditScreens
import net.chronoflow.feature.wizard.debug.GuestWizardBoardSeed
import net.chronoflow.feature.wizard.debug.WizardBoardCaptureSeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path

class AuditScreensTest {
    private val root =
        generateSequence(File(checkNotNull(System.getProperty("user.dir")))) { it.parentFile }
            .first { File(it, "tools/audit/board-scope.json").isFile }
    private val boards =
        Json
            .parseToJsonElement(File(root, "_design/canvas/canvas.json").readText())
            .jsonObject
            .getValue("boards")
            .jsonObject
            .mapKeys { it.key.removeSuffix(".dc.html") }
    private val scope = Json.parseToJsonElement(File(root, "tools/audit/board-scope.json").readText()).jsonObject
    private val excluded =
        scope
            .getValue("excluded")
            .jsonObject.values
            .flatMap { it.jsonArray }
            .map { FileSystems.getDefault().getPathMatcher("glob:${it.jsonPrimitive.content}") }

    @Test fun namesAreUnique() {
        assertEquals(auditScreens.size, auditScreens.map { it.name }.toSet().size)
    }

    @Test fun everyNameIsAnInScopeScreenBoard() {
        val nonScreens =
            scope
                .getValue("kinds")
                .jsonObject
                .getValue("nonScreen")
                .jsonObject.keys
        val notBuilt = scope.getValue("notBuilt").jsonObject.keys
        assertTrue(auditScreens.all { it.name in boards && inScope(it.name) && it.name !in nonScreens + notBuilt })
    }

    @Test fun everySeedKeyIsParsed() {
        assertTrue(
            auditScreens.all { screen ->
                val parsed = TestSeed.parse(screen.seeds + (TestSeed.TEST_ID_KEY to AUDIT_SEED_ID))
                parsed.ignoredKeys.isEmpty() &&
                    screen.seeds.all { (key, raw) ->
                        when (key) {
                            "CF_AUTH_TEST_SIGNED_IN" -> parsed.signedIn?.raw == raw
                            "CF_AUTH_TEST_WALKTHROUGH_SEEN" -> parsed.walkthroughSeen
                            "CF_PLAN_TEST_FREEZE_CLOCK" -> parsed.freezePlanClock
                            "CF_WALKTHROUGH_TEST_PENDING" -> parsed.walkthroughPending
                            "CF_AUTH_TEST_TERMS_PENDING" -> parsed.termsPending
                            "CF_PLAN_TEST_ROUTE" -> parsed.planRoute != null
                            "CF_PLAN_TEST_POSITION" -> parsed.planSummary
                            "CF_WIZARD_BOARD_CAPTURE" -> parsed.wizardBoardCapture == raw
                            "CF_WIZARD_AUTO_OPEN" -> parsed.wizardAutoOpen
                            "CF_GUEST_WIZARD" -> parsed.guestWizard
                            "CF_REFRESH_TEST_EMPTY_TRIPS" -> parsed.emptyTrips
                            "CF_AUTH_TEST_AUDIT_SCREEN" -> parsed.auditScreen == raw
                            "CF_AUTH_TEST_AUDIT_STATE" -> parsed.auditState == raw
                            "CF_SETTINGS_ROUTE" -> parsed.settingsRoute == raw
                            else -> false
                        }
                    }
            },
        )
    }

    @Test fun everyInScopeTripWizardSeedIsRegistered() {
        val expected =
            (WizardBoardCaptureSeed.names + GuestWizardBoardSeed.names)
                .filter {
                    boards[it]
                        ?.jsonObject
                        ?.get("page")
                        ?.jsonPrimitive
                        ?.content == "trip" && inScope(it)
                }.toSet()
        val actual = auditScreens.filter { it.name in expected }.map { it.name }.toSet()
        assertEquals(expected, actual)
    }

    @Test fun everySettingsRouteHasAnEntry() {
        assertEquals(
            setOf(
                "none",
                "profile",
                "account",
                "language",
                "security",
                "password",
                "twoFactor",
                "twoFactorSetup",
                "twoFactorSettings",
                "passkeys",
                "bodyClock",
                "preferences",
                "reminders",
                "plan",
                "delete",
            ),
            auditScreens.mapNotNull { it.seeds["CF_SETTINGS_ROUTE"] }.toSet(),
        )
    }

    private fun inScope(name: String): Boolean = excluded.none { it.matches(Path.of(name)) }
}
