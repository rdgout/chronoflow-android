package net.chronoflow.core.reminders

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.TimeFormat
import org.junit.Test

class ReminderSettingsTests {
    @Test fun defaultsAndMasterSwitch() {
        val defaults = ReminderSettings()
        assertThat(defaults).isEqualTo(ReminderSettings(true, true, true, true, 30))
        assertThat(defaults.enabled).isTrue()
        assertThat(defaults.settingEnabled(false)).isEqualTo(ReminderSettings(false, false, false, false, 30))
        assertThat(defaults.settingEnabled(false).enabled).isFalse()
        assertThat(ReminderSettings(false, false, false, true).enabled).isTrue()
        assertThat(ReminderSettings(leadMin = 60).settingEnabled(false).leadMin).isEqualTo(60)
    }

    @Test fun decodeIsStrict() {
        val valid = """{"light":true,"avoid":false,"caffeine":true,"winddown":true,"leadMin":15}"""
        assertThat(ReminderSettings.decode(valid)).isEqualTo(ReminderSettings(true, false, true, true, 15))
        assertThat(
            ReminderSettings.decode(valid.dropLast(1) + ",\"x\":1}"),
        ).isEqualTo(ReminderSettings(true, false, true, true, 15))
        for (invalid in listOf(
            valid.replace(",\"leadMin\":15", ""),
            valid.replace("\"light\":true", "\"light\":\"true\""),
            valid.replace("15", "\"30\""),
            valid.replace("15", "30.5"),
            valid.replace("15", "2147483648"),
            "{",
            "[]",
            null,
        )) {
            assertThat(ReminderSettings.decode(invalid), invalid).isNull()
        }
        assertThat(ReminderSettings.decode(valid.replace("15", "45"))?.leadMin).isEqualTo(45)
    }

    @Test fun timeFormatFromPreferences() {
        assertThat(reminderTimeFormat("""{"timeFormat":"12"}""")).isEqualTo(TimeFormat.Value12)
        for (value in listOf(
            """{"timeFormat":"24"}""",
            """{"timeFormat":12}""",
            """{"timeFormat":"other"}""",
            "{}",
            "{",
            "[]",
            null,
        )) {
            assertThat(reminderTimeFormat(value), value).isEqualTo(TimeFormat.Value24)
        }
    }

    @Test fun patchesAreExact() {
        assertThat(ApiJson.request.encodeToString(ReminderSettings.enabledPatch(true)))
            .isEqualTo("""{"light":true,"avoid":true,"caffeine":true,"winddown":true}""")
        assertThat(ApiJson.request.encodeToString(ReminderSettings.leadPatch(60)))
            .isEqualTo("""{"lead_min":60}""")
        assertThat(ReminderSettings.leadPatch(45)).isNull()
        assertThat(ReminderSettings.leadPatch(0)).isNull()
        for (minutes in listOf(15, 30, 60)) assertThat(ReminderSettings.isLeadOption(minutes)).isTrue()
    }
}
