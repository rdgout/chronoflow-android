package net.chronoflow.feature.settings.plan

import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.ui.R
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException

data class SettingsPlanLine(
    val key: String,
    val date: String,
    val interval: String,
)

object SettingsPlanText {
    val paymentFailedResource: Int get() = R.string.settings_update_your_payment_method_to_ke_b6a57d

    fun line(
        state: SettingsPlanState,
        formatter: CopyLineFormatter,
        language: Language,
        zone: ZoneId,
    ): SettingsPlanLine? {
        val raw = state.date ?: return null
        val date = parse(raw) ?: return null
        val formatted = formatter.formatDate(date, language, if (raw.length == 10) "UTC" else zone.id)
        return SettingsPlanLine(
            if (state.cancelled) {
                "Ends on {{date}}. After that, you can buy credits per trip."
            } else {
                "Renews on {{date}} · {{interval}}"
            },
            formatted,
            state.interval,
        )
    }

    private fun parse(raw: String): String? =
        try {
            Instant.parse(raw).toString()
        } catch (_: DateTimeParseException) {
            try {
                OffsetDateTime.parse(raw).toInstant().toString()
            } catch (_: DateTimeParseException) {
                try {
                    LocalDate
                        .parse(raw)
                        .atTime(12, 0)
                        .toInstant(java.time.ZoneOffset.UTC)
                        .toString()
                } catch (
                    _: DateTimeParseException,
                ) {
                    null
                }
            }
        }
}
