package net.chronoflow.feature.settings.plan

import kotlinx.serialization.json.JsonObject
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.ui.R
import net.chronoflow.feature.settings.support.SettingsFixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId

class SettingsPlanTextTests {
    private val formatter = CopyLineFormatter(SettingsFixture.catalog)

    private fun line(
        raw: String,
        language: Language = Language.En,
        cancelled: Boolean = false,
    ): SettingsPlanLine? {
        val billing =
            ApiJson.response.parseToJsonElement(
                """{"subscription":{"tier":"annual",
            "canResume":$cancelled,"${if (cancelled) "endsAt" else "renewsAt"}":"$raw"}}""",
            ) as JsonObject
        return SettingsPlanText.line(
            SettingsPlanState(null, billing, true, false),
            formatter,
            language,
            ZoneId.of("America/Los_Angeles"),
        )
    }

    @Test fun renewLineUsesTheFixedMonthTables() {
        val en = checkNotNull(line("2026-11-14T12:00:00Z"))
        assertEquals("Renews on {{date}} · {{interval}}", en.key)
        assertEquals("14 Nov 2026", en.date)
        assertEquals("yearly", en.interval)
        assertEquals("14 nov 2026", line("2026-11-14T12:00:00Z", Language.Nl)?.date)
    }

    @Test fun endsLineWhenCancelled() {
        assertEquals(
            "Ends on {{date}}. After that, you can buy credits per trip.",
            line("2026-11-14", cancelled = true)?.key,
        )
    }

    @Test fun dateOnlyAndFractionalTimestampsParse() {
        assertEquals("14 Nov 2026", line("2026-11-14")?.date)
        assertEquals("13 Nov 2026", line("2026-11-14T00:00:00.000000Z")?.date)
    }

    @Test fun missingOrBadDateDropsTheLine() {
        assertNull(
            SettingsPlanText.line(
                SettingsPlanState(null, null, true, false),
                formatter,
                Language.En,
                ZoneId.of("UTC"),
            ),
        )
        assertNull(line("bad"))
    }

    @Test fun paymentFailedUsesTheWebsiteFreeLine() {
        assertEquals(R.string.settings_update_your_payment_method_to_ke_b6a57d, SettingsPlanText.paymentFailedResource)
    }
}
