package net.chronoflow.feature.settings.passkeys

import android.content.res.Resources
import net.chronoflow.core.model.api.ApiPasskey
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.ui.R
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException

object SettingsPasskeyText {
    fun count(
        resources: Resources,
        count: Int,
    ): String = resources.getQuantityString(R.plurals.settings_n_passkeys, count, count)

    fun meta(
        passkey: ApiPasskey,
        language: Language,
        now: Instant,
        zone: ZoneId,
        formatter: CopyLineFormatter,
        text: (String, Map<String, String>) -> String,
    ): String? {
        val added = date(passkey.createdAt)
        val used = date(passkey.lastUsedAt)
        val parts = mutableListOf<String>()
        if (added != null) {
            parts += text("Added {{date}}", mapOf("date" to formatter.formatDate(added.toString(), language, zone.id)))
        }
        if (used != null) {
            val local = used.atZone(zone).toLocalDate()
            val current = now.atZone(zone).toLocalDate()
            val date =
                when {
                    local == current -> text("today", emptyMap())
                    local.year == current.year -> formatter.formatDayMonth(used.toString(), language, zone.id)
                    else -> formatter.formatDate(used.toString(), language, zone.id)
                }
            parts += text("used {{date}}", mapOf("date" to date))
        }
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
    }

    private fun date(raw: String?): Instant? {
        if (raw == null) return null
        return try {
            Instant.parse(raw)
        } catch (_: DateTimeParseException) {
            try {
                OffsetDateTime.parse(raw).toInstant()
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }
}
