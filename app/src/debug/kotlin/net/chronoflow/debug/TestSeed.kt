package net.chronoflow.debug

import android.content.Intent
import net.chronoflow.core.model.config.LanguageChoice
import net.chronoflow.core.model.config.ThemeChoice

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
) {
    companion object {
        const val TEST_ID_KEY = "CF_AUTH_TEST_ID"
        const val PROFILE_KEY = "CF_UI_TEST_PROFILE"

        private val canonicalUuid =
            Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
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
            return TestSeed(
                testId = testId,
                profile = UiTestProfile.entries.firstOrNull { it.raw == accepted[PROFILE_KEY] },
                theme = ThemeChoice.fromRaw(accepted["cf.settings.theme"]),
                language = LanguageChoice.fromRaw(accepted["cf.settings.language"]),
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
