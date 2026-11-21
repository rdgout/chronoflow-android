package net.chronoflow.feature.settings.bodyclock

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import net.chronoflow.core.model.contracts.ProfilePreferences

sealed interface BodyClockReadiness {
    data class NeedsAnswers(
        val missingCount: Int,
        val total: Int,
        val done: Int,
    ) : BodyClockReadiness {
        val titleKey: String get() =
            if (missingCount == 1) {
                "We need 1 more answer before we can build a plan"
            } else {
                "We need {{count}} more answers before we can build a plan"
            }
    }

    data class Excluded(
        val lineKeys: List<String>,
    ) : BodyClockReadiness

    data class Ready(
        val adjusted: Boolean,
        val adjustmentKeys: List<String>,
    ) : BodyClockReadiness

    companion object {
        fun of(readiness: JsonObject?): BodyClockReadiness? =
            when (readiness?.get("variant").stringValue()) {
                "excluded" -> {
                    Excluded(lines(readiness, "exclusions"))
                }

                "needs-answers" -> {
                    val invalid = (readiness?.get("invalidSections") as? JsonArray)?.size ?: 0
                    val missing = ((readiness?.get("missing") as? JsonArray)?.size ?: 0) + invalid
                    val total = 2 + invalid
                    NeedsAnswers(missing, total, (total - missing).coerceAtLeast(0))
                }

                "ready" -> {
                    Ready(false, emptyList())
                }

                "adjusted" -> {
                    Ready(true, lines(readiness, "adjustments"))
                }

                else -> {
                    null
                }
            }

        fun missingFields(readiness: JsonObject?): List<String> =
            (readiness?.get("missing") as? JsonArray)
                ?.map {
                    ProfilePreferences.camel((it as? JsonObject)?.get("question").stringValue() ?: "age_band")
                }.orEmpty()

        fun firstMissing(readiness: JsonObject?): String {
            missingFields(readiness).firstOrNull()?.let { return it }
            val invalid = (readiness?.get("invalidSections") as? JsonArray)?.firstOrNull().stringValue()
            return BodyClockGroup.entries
                .firstOrNull { it.raw == invalid }
                ?.fields
                ?.firstOrNull() ?: "ageBand"
        }

        private fun lines(
            value: JsonObject?,
            key: String,
        ): List<String> =
            (value?.get(key) as? JsonArray)?.mapNotNull { (it as? JsonObject)?.get("key").stringValue() }.orEmpty()
    }
}
