package net.chronoflow.core.reminders

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import net.chronoflow.core.plan.planDate
import org.junit.Test

class ReminderScheduleTests {
    @Test fun ledgerValidationAndExpiry() {
        val reminderNow = checkNotNull(planDate("2026-10-14T00:00:00Z"))
        for (text in listOf("{", "{}", """[null, {}, {"memberKeys": [1]}]""")) {
            assertThat(ReminderLedger.decode(text)).isEmpty()
        }
        val valid =
            ReminderLedgerEntry(
                "trip",
                "hash",
                reminderNow.toEpochMilli().toDouble(),
                listOf("seek_light|2026-10-14|1"),
            )
        val json =
            """{"tripId":"trip","inputHash":"hash","sendAtMs":${valid.sendAtMs},""" +
                """"memberKeys":["seek_light|2026-10-14|1"]}"""
        assertThat(ReminderLedger.decode("[$json]")).isEqualTo(listOf(valid))
        assertThat(ReminderLedger.elapsed(listOf(valid), reminderNow)).isEqualTo(listOf(valid))
        assertThat(ReminderLedger.elapsed(listOf(valid), reminderNow.plusSeconds(36 * 3600 + 1))).isEmpty()
        assertThat(ReminderLedger.elapsed(listOf(valid), reminderNow.minusSeconds(1))).isEmpty()
        assertThat(
            ReminderLedger.decode("""[{"tripId":"t","inputHash":"h","sendAtMs":true,"memberKeys":[]}]"""),
        ).isEmpty()
        assertThat(ReminderLedger.elapsed(listOf(valid), reminderNow.plusSeconds(36 * 3600))).isEqualTo(listOf(valid))
        for (invalid in listOf(
            json.replace(valid.sendAtMs.toString(), "\"123\""),
            json.replace(valid.sendAtMs.toString(), "1e999"),
            json.replace("\"trip\"", "1"),
            json.replace("\"hash\"", "null"),
            json.replace("\"seek_light|2026-10-14|1\"", "1"),
        )) {
            assertThat(ReminderLedger.decode("[$invalid,$json]")).isEqualTo(listOf(valid))
        }
    }
}
