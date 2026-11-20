package net.chronoflow.feature.settings.home

import net.chronoflow.core.model.CheckOffChange
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.store.AccountRecord
import net.chronoflow.core.model.store.OutboxPayload
import net.chronoflow.core.model.store.OutboxRecord
import net.chronoflow.core.model.store.SettingsRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class SettingsHomeStateTests {
    private val now = Instant.ofEpochSecond(1_794_132_000)

    private fun state(age: Long) = SettingsHomeState(account = account(syncedAt = now.minusSeconds(age).toString()))

    private fun line(age: Long) = state(age).syncLine(now, zone = ZoneOffset.UTC)

    @Test fun syncedLineAgeAndPriorityMatchRN() {
        assertEquals(SyncLine.JustNow, line(0))
        assertEquals(SyncLine.JustNow, line(-60))
        assertEquals(SyncLine.JustNow, line(59))
        assertEquals(SyncLine.Minutes(1), line(60))
        assertEquals(SyncLine.Minutes(59), line(3599))
        assertEquals(SyncLine.Hours(1), line(3600))
        assertEquals(SyncLine.Hours(23), line(86399))
        assertEquals(SyncLine.Yesterday, line(86400))
        assertEquals(SyncLine.Days(2), line(172800))
        assertEquals(SyncLine.On(now.minusSeconds(604800).toString()), line(604800))
        val value = state(120)
        assertEquals(SyncLine.Offline, value.syncLine(now, offline = true, zone = ZoneOffset.UTC))
        val pending = value.copy(pendingCount = 2)
        assertEquals(SyncLine.Pending(2), pending.syncLine(now, offline = true, zone = ZoneOffset.UTC))
        assertEquals(SyncLine.Syncing, pending.syncLine(now, syncing = true, offline = true, zone = ZoneOffset.UTC))
        assertNull(value.copy(account = account(syncedAt = "invalid")).syncLine(now, zone = ZoneOffset.UTC))
        assertNull(SettingsHomeState().syncLine(now, zone = ZoneOffset.UTC))
    }

    @Test fun initialsReadinessAndCreditsFollowLocalValues() {
        val value = state(0)
        assertEquals("RG", value.initials)
        assertEquals("É", value.copy(account = account("éva")).initials)
        assertEquals("🧑🏽‍💻V", value.copy(account = account("🧑🏽‍💻 de Vries")).initials)
        assertEquals("", value.copy(account = account("  ")).initials)
        val loaded =
            value.apply(
                settings =
                    listOf(
                        SettingsRecord(
                            "readiness",
                            """{"variant":"needs-answers","missing":["age","shift"],"invalidSections":["sleep"]}""",
                        ),
                        SettingsRecord("planCredits", """{"credits":2,"unlimited":false}"""),
                    ),
            )
        assertEquals(3, loaded.missingAnswers)
        assertEquals(2, loaded.credits)
        val unlimited =
            loaded.apply(
                settings = listOf(SettingsRecord("planCredits", """{"credits":2,"unlimited":true}""")),
            )
        assertEquals(0, unlimited.missingAnswers)
        assertNull(unlimited.credits)
        assertEquals(0, loaded.apply(settings = listOf(SettingsRecord("planCredits", """{"credits":0}"""))).credits)
        assertNull(loaded.apply(settings = emptyList()).credits)
    }

    @Test fun pendingChangesGroupSettingsAndUseLatestCheckAcrossHashes() {
        val value =
            SettingsHomeState().apply(
                outbox =
                    listOf(
                        check("1"),
                        setting("2", "bodyClock"),
                        setting("3", "bodyClock"),
                        check("4", hash = "new"),
                        setting("5", "preferences"),
                    ),
            )
        assertEquals(5, value.pendingCount)
        assertEquals(listOf("4", "2", "5"), value.pending.map { it.id })
        assertEquals(PendingText.CheckedOff(BlockType.AvoidLight), value.pending[0].text)
        assertEquals(BlockType.Sleep, value.pending[1].category)
        assertEquals(PendingText.Sleep, value.pending[1].text)
        assertEquals(PendingText.Generic, value.pending[2].text)
        val undone = SettingsHomeState().apply(outbox = listOf(check("1"), check("2", status = null)))
        assertEquals(1, undone.pending.size)
        assertEquals(PendingText.Generic, undone.pending.single().text)
    }
}

internal fun account(
    name: String = "  Rick van Gout  ",
    syncedAt: String? = null,
) = AccountRecord("account", name, "r@example.test", syncedAt = syncedAt)

internal fun check(
    id: String,
    status: CheckStatus? = CheckStatus.Done,
    hash: String = "old",
    type: BlockType = BlockType.AvoidLight,
) = OutboxPayload.record(OutboxChange.CheckOff(CheckOffChange("trip", hash, type, "2026-11-08", 1, status)), id, id)

internal fun setting(
    id: String,
    section: String,
    patch: String = """{"habitual_bed":"23:00"}""",
) = OutboxRecord(id, "settings", """{"section":"$section","patch":$patch}""", id)
