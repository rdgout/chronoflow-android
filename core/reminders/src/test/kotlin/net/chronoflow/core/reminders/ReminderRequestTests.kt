package net.chronoflow.core.reminders

import assertk.assertThat
import assertk.assertions.isEqualTo
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TimeFormat
import org.junit.Test

class ReminderRequestTests : ReminderTestSupport(formatter) {
    companion object {
        private val formatter by lazy { reminderFormatter() }
    }

    @Test fun identifierGolden() {
        val blocks =
            listOf(
                reminderBlock(),
                reminderBlock(1.1, BlockType.AvoidLight),
                reminderBlock(1.2, BlockType.NoCaffeine),
                reminderBlock(1.25, BlockType.Sleep, zone = "Europe/Amsterdam"),
            )
        val request = scheduled(listOf(reminderPlan(blocks))).first()
        assertThat(request.identifier).isEqualTo(
            "chronoflow.reminder.MDE5MDAwMDAtMDAwMC03MDAwLTgwMDAtMDAwMDAwMDAwMDAxCnRyaXAKaGFzaApzZWVrX2xpZ2h0" +
                "fDIwMjYtMTAtMTR8MQphdm9pZF9saWdodHwyMDI2LTEwLTE0fDEKbm9fY2FmZmVpbmV8MjAyNi0xMC0xNHwx" +
                "CnNsZWVwfDIwMjYtMTAtMTR8MQ==",
        )
        assertThat(request.memberKeys).isEqualTo(
            listOf(
                "seek_light|2026-10-14|1",
                "avoid_light|2026-10-14|1",
                "no_caffeine|2026-10-14|1",
                "sleep|2026-10-14|1",
            ),
        )
        assertThat(scheduled(listOf(reminderPlan())).first().identifier).isEqualTo(
            "chronoflow.reminder.MDE5MDAwMDAtMDAwMC03MDAwLTgwMDAtMDAwMDAwMDAwMDAxCnRyaXAKaGFzaApzZWVrX2xpZ2h0" +
                "fDIwMjYtMTAtMTR8MQ==",
        )
    }

    @Test fun dutchTwelveHourSentenceEndsWithOneDot() {
        val result =
            scheduled(
                listOf(reminderPlan(listOf(reminderBlock(1.25, BlockType.Sleep)))),
                language = Language.Nl,
                timeFormat = TimeFormat.Value12,
            )
        assertThat(result.first().body).isEqualTo("Kom tot rust: slapen vanaf 01:15 a.m.")
    }

    @Test fun requestCarriesTripBlockHashAccountAndExpiry() {
        val result = scheduled(listOf(reminderPlan())).single()
        assertThat(result.tripID).isEqualTo("trip")
        assertThat(result.blockID).isEqualTo("seek_light-1.0")
        assertThat(result.inputHash).isEqualTo("hash")
        assertThat(result.accountID).isEqualTo(reminderAccount)
        assertThat(result.sendAt).isEqualTo(reminderNow.plusSeconds(30 * 60))
        assertThat(result.expiresAt).isEqualTo(reminderNow.plusSeconds(80 * 60))
    }

    @Test fun blockIdFallsBackToTypeAndStart() {
        val block = reminderBlock()
        assertThat(scheduled(listOf(reminderPlan(listOf(block)))).single().blockID).isEqualTo(block.id)
        assertThat(
            scheduled(listOf(reminderPlan(listOf(block.copy(checkKey = null))))).single().blockID,
        ).isEqualTo(block.id)
    }

    @Test fun ledgerEntryFromRequest() {
        val request = scheduled(listOf(reminderPlan())).single()
        assertThat(ReminderLedgerEntry.from(request)).isEqualTo(
            ReminderLedgerEntry(
                request.tripID,
                request.inputHash,
                request.sendAt.toEpochMilli().toDouble(),
                request.memberKeys,
            ),
        )
    }

    @Test fun missingCopyFallsBackToTheKey() {
        val strings = ReminderStrings { key, _, time -> key.replace("{{time}}", time) }
        val plans = listOf(reminderPlan())
        assertThat(scheduled(plans, language = Language.Nl, strings = strings).single().body)
            .isEqualTo(scheduled(plans, language = Language.En, strings = strings).single().body)
    }
}
