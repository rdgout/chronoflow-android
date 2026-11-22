package net.chronoflow.core.reminders

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.Priority
import net.chronoflow.core.plan.planDate
import org.junit.Test
import java.util.Locale
import java.util.TimeZone

class ReminderSelectionEdgeTests {
    @Test fun localDayUsesTheZoneAndFallsBackToUtc() {
        for ((instant, zone, day) in listOf(
            Triple("2026-10-14T15:30Z", "Asia/Tokyo", "2026-10-15"),
            Triple("2026-11-01T03:30Z", "America/New_York", "2026-10-31"),
            Triple("2026-10-25T22:30Z", "Europe/Amsterdam", "2026-10-25"),
            Triple("2026-10-14T23:30Z", "Not/AZone", "2026-10-14"),
            Triple("1969-12-31T23:30:00Z", "Asia/Tokyo", "1970-01-01"),
        )) {
            assertThat(reminderLocalDay(checkNotNull(planDate(instant)), zone)).isEqualTo(day)
        }
    }

    @Test fun unparseableBlocksAreIgnored() {
        val block =
            ReminderSelectionTests.vectors
                .asSequence()
                .flatMap { it.blocks.asSequence() }
                .first { it.type == BlockType.SeekLight && it.actionable }
                .copy(priority = Priority.Core, checkKey = null)
        val now = checkNotNull(planDate(block.startUtc)).minusSeconds(3600)
        val invalid = block.copy(id = "invalid", startUtc = "garbage")
        assertThat(reminderDrafts(listOf(invalid), ReminderSettings(), now)).isEmpty()
        val result = reminderDrafts(listOf(invalid, block), ReminderSettings(), now)
        assertThat(
            result
                .single()
                .members
                .single()
                .ordinal,
        ).isEqualTo(1)
        val invalidEnd = invalid.copy(startUtc = block.startUtc, endUtc = "garbage")
        assertThat(reminderDrafts(listOf(invalidEnd, block), ReminderSettings(), now)).isEqualTo(result)
    }

    @Test fun outputIgnoresDefaultLocaleAndZone() {
        val previousLocale = Locale.getDefault()
        val previousZone = TimeZone.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ar"))
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"))
            for (vector in ReminderSelectionTests.vectors.take(10)) {
                assertThat(reminderDrafts(vector.blocks, vector.settings, vector.now, vector.options), vector.name)
                    .isEqualTo(vector.expected)
            }
        } finally {
            Locale.setDefault(previousLocale)
            TimeZone.setDefault(previousZone)
        }
    }
}
