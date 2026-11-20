package net.chronoflow.feature.settings.home

import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.store.OutboxRecord
import net.chronoflow.core.model.store.SettingsRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class SettingsHomeStateEdgeTests {
    @Test fun fractionalSecondSyncedAt() {
        val state = SettingsHomeState(account = account(syncedAt = "2026-11-08T10:00:00.123Z"))
        assertEquals(SyncLine.Minutes(1), state.syncLine(Instant.parse("2026-11-08T10:01:01Z"), zone = ZoneOffset.UTC))
    }

    @Test fun malformedReadinessResetsValues() {
        val state =
            SettingsHomeState(
                missingAnswers = 3,
                credits = 2,
            ).apply(settings = listOf(SettingsRecord("readiness", "{")))
        assertEquals(0, state.missingAnswers)
        assertNull(state.credits)
    }

    @Test fun negativeCreditsAreAbsent() {
        assertNull(
            SettingsHomeState().apply(settings = listOf(SettingsRecord("planCredits", """{"credits":-1}"""))).credits,
        )
    }

    @Test fun nonNumericCreditsAreAbsent() {
        assertNull(
            SettingsHomeState().apply(settings = listOf(SettingsRecord("planCredits", """{"credits":"2"}"""))).credits,
        )
    }

    @Test fun dedupeKeepsFirstSettingsPositionAndValue() {
        val state =
            SettingsHomeState().apply(
                outbox = listOf(setting("1", "bodyClock"), check("2"), setting("3", "bodyClock")),
            )
        assertEquals(listOf("1", "2"), state.pending.map { it.id })
    }

    @Test fun unknownStepLabelIsGeneric() {
        val item =
            SettingsHomeState()
                .apply(
                    outbox = listOf(check("1", type = BlockType.Unknown("x"))),
                ).pending
                .single()
        assertEquals(PendingText.Generic, item.text)
        assertEquals(BlockType.Unknown("x"), item.category)
    }

    @Test fun explicitSleepSectionWinsOverNonSleepPatch() {
        val row =
            OutboxRecord(
                "1",
                "settings",
                """{"section":"bodyClock","bodyClockSection":"sleep","patch":{"age_band":"adult"}}""",
                "1",
            )
        assertEquals(
            PendingText.Sleep,
            SettingsHomeState()
                .apply(outbox = listOf(row))
                .pending
                .single()
                .text,
        )
    }

    @Test fun invalidPayloadFallsThroughToGeneric() {
        val row = OutboxRecord("1", "check-off", "{", "1")
        assertEquals(
            PendingText.Generic,
            SettingsHomeState()
                .apply(outbox = listOf(row))
                .pending
                .single()
                .text,
        )
    }

    @Test fun daysUseLocalDatesAcrossDaylightSaving() {
        val state = SettingsHomeState(account = account(syncedAt = "2026-03-28T22:30:00Z"))
        assertEquals(
            SyncLine.Days(2),
            state.syncLine(Instant.parse("2026-03-30T00:30:00Z"), zone = java.time.ZoneId.of("Europe/Amsterdam")),
        )
    }
}
