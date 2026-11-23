package net.chronoflow.debug

import android.content.Intent
import net.chronoflow.core.model.config.LanguageChoice
import net.chronoflow.core.model.config.ThemeChoice
import net.chronoflow.core.navigation.TripSheet
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

enum class UiTestProfile(
    val raw: String,
) {
    Screen("screen"),
    Launch("launch"),
}

/**
 * The seed an emulator test sends as string intent extras (ports iOS AuthUITestConfiguration and LaunchArguments).
 * `CF_*` keys count only with a valid `CF_AUTH_TEST_ID`; `cf.*` keys (iOS launch arguments) apply without it.
 */
data class TestSeed(
    val testId: String?,
    val profile: UiTestProfile?,
    val theme: ThemeChoice?,
    val language: LanguageChoice?,
    val reduceMotion: Boolean?,
    val holdSplash: Boolean?,
    val freezeAtMs: Double?,
    /** Verbatim, because the plan clock (1d) reads an ISO-8601 instant from the same key. */
    val freezeAtRaw: String?,
    val readyAtMs: Double?,
    val mainThreadStallMs: Double?,
    val raw: Map<String, String>,
    val ignoredKeys: Set<String>,
    val signedIn: SeedAccount? = null,
    val walkthroughSeen: Boolean = false,
    val termsPending: Boolean = false,
    val unverified: Boolean = false,
    val auditScreen: String? = null,
    val auditState: String? = null,
    val mockDelay: Duration = Duration.ZERO,
    val planRoute: PlanRouteSeed? = null,
    val planSummary: Boolean = false,
    val freezePlanClock: Boolean = false,
    val planSheet: TripSheet? = null,
    val settingsRoute: String? = null,
    val wizardAutoOpen: Boolean = false,
    val wizardBoardCapture: String? = null,
    val passkeyCapability: Boolean? = null,
    val authTestLink: String? = null,
    val walkthroughPending: Boolean = false,
    val termsChangesPending: Boolean = false,
    val settingsPending: Boolean = false,
    val settingsBoardState: String? = null,
    val guestWizard: Boolean = false,
    val guestWizardJourney: Boolean = false,
    val guestRecoveryBoard: String? = null,
) {
    val mutationsOffline: Boolean get() = walkthroughPending || termsPending || settingsPending

    override fun toString(): String = "TestSeed(testId=$testId, profile=$profile, keys=${raw.keys})"

    companion object {
        const val TEST_ID_KEY = "CF_AUTH_TEST_ID"
        const val PROFILE_KEY = "CF_UI_TEST_PROFILE"

        private val canonicalUuid =
            Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
        private val settingsRoutes =
            setOf(
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
            )
        private const val MAX_MS = 60_000.0

        fun parse(
            extras: Map<String, String>,
            nonStringKeys: Set<String> = emptySet(),
        ): TestSeed {
            val testId = extras[TEST_ID_KEY]?.takeIf { canonicalUuid.matches(it) }?.lowercase()
            val ignored = nonStringKeys.toMutableSet()
            val accepted = LinkedHashMap<String, String>()
            for ((key, value) in extras) {
                when {
                    key.startsWith("cf.") -> accepted[key] = value
                    key.startsWith("CF_") -> if (testId != null) accepted[key] = value else ignored += key
                }
            }

            fun ms(key: String): Double? =
                accepted[key]?.toDoubleOrNull()?.takeIf { it.isFinite() && it in 0.0..MAX_MS }

            fun <T> parsed(
                key: String,
                parse: (String) -> T?,
            ): T? {
                val value = accepted[key] ?: return null
                return parse(value).also { if (it == null) ignored += key }
            }

            fun flag(key: String): Boolean =
                parsed(key) {
                    when (it) {
                        "1" -> true
                        "0" -> false
                        else -> null
                    }
                } ?: false
            val signedIn =
                parsed("CF_AUTH_TEST_SIGNED_IN") { value -> SeedAccount.entries.firstOrNull { it.raw == value } }
            val walkthroughSeen = flag("CF_AUTH_TEST_WALKTHROUGH_SEEN")
            val termsPending = flag("CF_AUTH_TEST_TERMS_PENDING")
            val walkthroughPending = flag("CF_WALKTHROUGH_TEST_PENDING")
            val termsChangesPending = flag("CF_AUTH_TEST_TERMS_CHANGES_PENDING")
            val settingsPending = flag("CF_SETTINGS_TEST_PENDING")
            val unverified = flag("CF_AUTH_TEST_UNVERIFIED")
            val mockDelay =
                parsed("CF_MOCK_DELAY") {
                    it.toDoubleOrNull()?.takeIf { delay -> delay.isFinite() && delay in 0.0..60.0 }
                } ?: 0.0
            val planRoute =
                parsed("CF_PLAN_TEST_ROUTE") { value ->
                    if (value == "trips") {
                        PlanRouteSeed.Trips
                    } else {
                        val parts = value.split(':')
                        val valid =
                            parts.size in 2..3 && parts[0] == "trip" && canonicalUuid.matches(parts[1]) &&
                                (parts.size == 2 || parts[2] == "list")
                        if (valid) PlanRouteSeed.Trip(parts[1].lowercase(), parts.size == 3) else null
                    }
                }
            val planSummary = parsed("CF_PLAN_TEST_POSITION") { if (it == "summary") true else null } ?: false
            val freezePlanClock = parsed("CF_PLAN_TEST_FREEZE_CLOCK") { if (it == "1") true else null } ?: false
            val planSheet =
                parsed("CF_PLAN_TEST_SHEET") { value ->
                    when {
                        value == "legend" -> TripSheet.StepsLegend

                        value == "priorities" -> TripSheet.Legend

                        value.startsWith(
                            "step:",
                        ) && value.substringAfter("step:").isNotBlank() -> TripSheet.Step(value.substringAfter("step:"))

                        else -> null
                    }
                }
            val settingsRoute = parsed("CF_SETTINGS_ROUTE") { it.takeIf { route -> route in settingsRoutes } }
            val settingsBoardState =
                parsed("CF_SETTINGS_BOARD_STATE") {
                    it.takeIf { state -> state in setOf("bodyMissing", "bodyReady", "bodyAdjusted", "bodyExcluded") }
                }
            val wizardAutoOpen = flag("CF_WIZARD_AUTO_OPEN")
            return TestSeed(
                testId = testId,
                profile = UiTestProfile.entries.firstOrNull { it.raw == accepted[PROFILE_KEY] },
                theme = ThemeChoice.fromRaw(accepted["cf.settings.theme"]),
                language = LanguageChoice.fromRaw(accepted["cf.settings.language"]),
                passkeyCapability =
                    when (accepted["cf.passkeyCapability"]) {
                        "1", "true" -> true
                        "0", "false" -> false
                        else -> null
                    },
                reduceMotion =
                    when (accepted["cf.reduceMotion"]) {
                        "1", "true" -> true
                        "0", "false" -> false
                        else -> null
                    },
                holdSplash =
                    when (accepted["cf.holdSplash"]) {
                        "1", "true" -> true
                        "0", "false" -> false
                        else -> null
                    },
                freezeAtMs = ms("cf.freezeAt"),
                freezeAtRaw = accepted["cf.freezeAt"],
                readyAtMs = ms("cf.readyAt"),
                mainThreadStallMs = ms("cf.mainThreadStall"),
                raw = accepted.filterKeys { it !in nonStringKeys },
                ignoredKeys = ignored,
                signedIn = signedIn,
                walkthroughSeen = walkthroughSeen,
                termsPending = termsPending,
                walkthroughPending = walkthroughPending,
                termsChangesPending = termsChangesPending,
                settingsPending = settingsPending,
                unverified = unverified,
                authTestLink = parsed("CF_AUTH_TEST_LINK") { it.takeIf { url -> url.startsWith("https:") } },
                auditScreen = accepted["CF_AUTH_TEST_AUDIT_SCREEN"],
                auditState = accepted["CF_AUTH_TEST_AUDIT_STATE"],
                mockDelay = mockDelay.seconds,
                planRoute = planRoute,
                planSummary = planSummary,
                freezePlanClock = freezePlanClock,
                planSheet = planSheet,
                settingsRoute = settingsRoute,
                settingsBoardState = settingsBoardState,
                wizardAutoOpen = wizardAutoOpen,
                wizardBoardCapture = accepted["CF_WIZARD_BOARD_CAPTURE"],
                guestWizard = flag("CF_GUEST_WIZARD"),
                guestWizardJourney = flag("CF_GUEST_WIZARD_JOURNEY"),
                guestRecoveryBoard = accepted["CF_GUEST_RECOVERY_BOARD"],
            )
        }

        /** String extras only; any other extra lands in [ignoredKeys]. */
        fun fromIntent(intent: Intent?): TestSeed {
            val extras = intent?.extras ?: return parse(emptyMap())
            val strings = LinkedHashMap<String, String>()
            val others = mutableSetOf<String>()
            for (key in extras.keySet()) {
                val value = extras.getString(key)
                if (value != null) strings[key] = value else others += key
            }
            return parse(strings, others)
        }
    }
}
