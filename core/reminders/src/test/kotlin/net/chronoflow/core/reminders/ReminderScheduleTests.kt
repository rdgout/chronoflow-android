package net.chronoflow.core.reminders

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import net.chronoflow.core.model.contracts.BlockReasonTagsView
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.CheckMark
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.CopyLine
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.Priority
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.model.contracts.TripStatus
import net.chronoflow.core.plan.planDate
import org.junit.Test

class ReminderScheduleTests : ReminderTestSupport(formatter) {
    companion object {
        private val formatter by lazy { reminderFormatter() }
    }

    @Test fun ledgerValidationAndExpiry() {
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

    @Test fun windowMarginAndCap() {
        val dates =
            scheduled(
                listOf(
                    reminderPlan(
                        listOf(-1.0, 0.0, 24.0, 48.5, 48.5001).mapIndexed { index, hours ->
                            reminderBlock(hours, ordinal = index + 1)
                        },
                    ),
                ),
            ).map { it.sendAt }
        assertThat(
            dates,
        ).isEqualTo(listOf(reminderNow.plusSeconds(23 * 3600 + 1800), reminderNow.plusSeconds(48 * 3600)))
        for (milliseconds in listOf(-1, 0, 1, 4999, 5000, 5001)) {
            val result = scheduled(listOf(reminderPlan(listOf(reminderBlock(0.5 + milliseconds / 3_600_000.0)))))
            assertThat(result.size, "$milliseconds ms").isEqualTo(if (milliseconds > 5000) 1 else 0)
        }
        val requests =
            scheduled(
                (0 until 65).map {
                    reminderPlan(listOf(reminderBlock(1 + it / 100.0)), id = "trip-$it")
                },
            )
        assertThat(requests.size).isEqualTo(60)
        assertThat(requests.map { it.sendAt }).isEqualTo(requests.map { it.sendAt }.sorted())
    }

    @Test fun elapsedDailyCapAndDST() {
        for ((zone, midnight) in listOf(
            "UTC" to "2026-10-14T00:00:00Z",
            "Pacific/Kiritimati" to "2026-10-13T10:00:00Z",
            "Pacific/Pago_Pago" to "2026-10-14T11:00:00Z",
            "Europe/Amsterdam" to "2026-10-24T22:00:00Z",
        )) {
            val start = checkNotNull(planDate(midnight))
            val blocks =
                (0 until 8).map { index ->
                    reminderBlock((index * 3 + 1).toDouble(), ordinal = index + 1, zone = zone).copy(
                        startUtc = start.plusSeconds((index * 3 + 1) * 3600L).toString(),
                        endUtc = start.plusSeconds((index * 3 + 2) * 3600L).toString(),
                    )
                }
            val first = scheduled(listOf(reminderPlan(blocks)), now = start)
            assertThat(first.size, zone).isEqualTo(6)
            val later = scheduled(listOf(reminderPlan(blocks)), now = start.plusSeconds(8 * 3600))
            assertThat(later.map { it.sendAt }, zone).isEqualTo(first.drop(3).map { it.sendAt })
        }
    }

    @Test fun ledgerSuppressesMergedMembersAndScopesTripAndHash() {
        val blocks = listOf(reminderBlock(), reminderBlock(1.1, BlockType.AvoidLight))
        val first = scheduled(listOf(reminderPlan(blocks))).first()
        val ledger =
            listOf(ReminderLedgerEntry("trip", "hash", first.sendAt.toEpochMilli().toDouble(), first.memberKeys))
        val now = first.sendAt.plusSeconds(1)
        assertThat(scheduled(listOf(reminderPlan(blocks)), now = now, ledger = ledger)).isEmpty()
        assertThat(scheduled(listOf(reminderPlan(blocks, id = "other")), now = now, ledger = ledger).size).isEqualTo(1)
        assertThat(
            scheduled(listOf(reminderPlan(blocks, hash = "other")), now = now, ledger = ledger).size,
        ).isEqualTo(1)
        val futureOnly = scheduled(listOf(reminderPlan(blocks)), now = now)
        assertThat(futureOnly.first().memberKeys).isEqualTo(listOf("avoid_light|2026-10-14|1"))
    }

    @Test fun checksOptionalAndIfYouCan() {
        val blocks = listOf(reminderBlock(), reminderBlock(2.0, ordinal = 2), reminderBlock(3.0, ordinal = 3))
        for (status in listOf(CheckStatus.Done, CheckStatus.Skipped)) {
            val mark = CheckMark(checkNotNull(blocks[1].checkKey), status, "2026-10-14T00:00:00Z")
            val result = scheduled(listOf(reminderPlan(blocks, marks = listOf(mark))))
            assertThat(result.flatMap { it.memberKeys })
                .isEqualTo(listOf("seek_light|2026-10-14|1", "seek_light|2026-10-14|3"))
        }
        val plan = reminderPlan(listOf(reminderBlock(1.0, BlockType.AvoidLight), reminderBlock(2.0)))
        val optional =
            plan.detail.plan.blocks[1]
                .copy(priority = Priority.Optional)
        val reason =
            BlockReasonTagsView(
                blockId =
                    plan.detail.plan.blocks[0]
                        .id,
                lines = listOf(CopyLine("If you can", emptyList())),
            )
        val reasons = listOf(reason)
        val filtered =
            plan.copy(
                detail =
                    plan.detail.copy(
                        timeline = plan.detail.timeline.copy(blockReasons = reasons),
                        plan = plan.detail.plan.copy(blocks = listOf(plan.detail.plan.blocks[0], optional)),
                    ),
            )
        assertThat(scheduled(listOf(filtered))).isEmpty()
        val completed =
            reminderPlan().let {
                it.copy(card = it.card.copy(progress = it.card.progress.copy(status = TripStatus.Completed)))
            }
        assertThat(scheduled(listOf(completed))).isEmpty()
    }

    @Test fun copyKindsZonesFormatsLanguagesAndStableIdentity() {
        val blocks =
            listOf(
                reminderBlock(),
                reminderBlock(1.1, BlockType.AvoidLight),
                reminderBlock(1.2, BlockType.NoCaffeine),
                reminderBlock(1.25, BlockType.Sleep, zone = "Europe/Amsterdam"),
            )
        val en = scheduled(listOf(reminderPlan(blocks))).first()
        assertThat(en.body).isEqualTo(
            "Seek bright light from 01:00. Avoid bright light from 01:06. " +
                "Last caffeine before 01:12. Wind down for bed: sleep from 03:15 (Amsterdam).",
        )
        val nl =
            scheduled(
                plans = listOf(reminderPlan(blocks)),
                language = Language.Nl,
                timeFormat = TimeFormat.Value12,
            ).first()
        assertThat(nl.body).isEqualTo(
            "Zoek vanaf 01:00 a.m. fel licht. Vermijd vanaf 01:06 a.m. fel licht. " +
                "Neem vóór 01:12 a.m. je laatste cafeïne. Kom tot rust: slapen vanaf 03:15 a.m. (Amsterdam).",
        )
        assertThat(scheduled(listOf(reminderPlan(blocks.reversed()))).first().body).isEqualTo(en.body)
        assertThat(en.identifier).isEqualTo(nl.identifier)
        assertThat(en.title).isEqualTo("Amsterdam → Tokyo")
        for (from in listOf("Amsterdam", null)) {
            for (to in listOf("Tokyo", null)) {
                val base = reminderPlan()
                val card =
                    base.card.copy(
                        from = base.card.from.copy(city = from),
                        to = base.card.to.copy(city = to),
                    )
                val plan = base.copy(card = card)
                assertThat(scheduled(listOf(plan)).first().title).isEqualTo("${from ?: "AMS"} → ${to ?: "HND"}")
            }
        }
        val duplicateKind = scheduled(listOf(reminderPlan(listOf(reminderBlock(1.1, ordinal = 2), reminderBlock()))))
        assertThat(duplicateKind.first().body).isEqualTo("Seek bright light from 01:00.")
        for ((format, time) in listOf(TimeFormat.Value12 to "01:30 PM", TimeFormat.Value24 to "13:30")) {
            val plan = reminderPlan(listOf(reminderBlock(4.5, zone = "Asia/Tokyo")))
            val result = scheduled(listOf(plan), timeFormat = format)
            assertThat(result.first().body).isEqualTo("Seek bright light from $time.")
        }
        for ((zone, time) in listOf(
            "Europe/Amsterdam" to "03:06 (Amsterdam)",
            "America/Los_Angeles" to "18:06 (Los Angeles)",
        )) {
            val result =
                scheduled(
                    listOf(
                        reminderPlan(
                            listOf(
                                reminderBlock(),
                                reminderBlock(1.1, BlockType.AvoidLight, zone = zone),
                            ),
                        ),
                    ),
                )
            assertThat(result.first().body).isEqualTo("Seek bright light from 01:00. Avoid bright light from $time.")
        }
    }
}
