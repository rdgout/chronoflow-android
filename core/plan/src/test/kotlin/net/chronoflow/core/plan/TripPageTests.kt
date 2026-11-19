package net.chronoflow.core.plan

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.CheckOffChange
import net.chronoflow.core.model.contracts.CheckMark
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.DayView
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.plan.trippage.TripPageAnchor
import net.chronoflow.core.plan.trippage.TripPageFetch
import net.chronoflow.core.plan.trippage.TripPageState
import net.chronoflow.core.plan.trippage.openingDay
import net.chronoflow.core.plan.trippage.replacementAnchor
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

class TripPageTests {
    @Test fun pendingOldHashMarksAndUndoOverrideTheNewPlan() =
        runTest(timeout = 60.seconds) {
            var detail = tripDetail()
            val key =
                checkNotNull(
                    detail.plan.blocks
                        .first {
                            it.actionable && it.checkKey != null &&
                                !isOptionalStep(it)
                        }.checkKey,
                )
            detail =
                detail.copy(
                    checkState =
                        detail.checkState.copy(
                            marks = listOf(CheckMark(key, CheckStatus.Done, detail.trip.departureUtc)),
                        ),
                )
            val old =
                CheckOffChange(
                    detail.trip.id,
                    "previous-hash",
                    key.type,
                    key.localDate,
                    key.ordinal,
                    CheckStatus.Skipped,
                )
            val preparer = preparer()
            val marked =
                preparer.prepare(
                    detail,
                    emptyList(),
                    Language.En,
                    TimeFormat.Value24,
                    emptyList(),
                    listOf(old),
                )
            assertThat(
                marked.days
                    .flatMap { it.blocks }
                    .first { it.block.checkKey == key }
                    .status,
            ).isEqualTo(CheckStatus.Skipped)
            val undo = old.copy(status = null)
            val undone =
                preparer.prepare(
                    detail,
                    emptyList(),
                    Language.En,
                    TimeFormat.Value24,
                    emptyList(),
                    listOf(old, undo),
                )
            assertThat(
                undone.days
                    .flatMap { it.blocks }
                    .first { it.block.checkKey == key }
                    .status,
            ).isNull()
            assertThat(preparer.layoutCount).isEqualTo(1)
            detail = detail.copy(detailVersion = detail.detailVersion + "-replacement")
            preparer.prepare(detail, emptyList(), Language.En, TimeFormat.Value24, emptyList())
            assertThat(preparer.layoutCount).isEqualTo(2)
        }

    @Test fun preparationCachesLayoutsAndPreservesMarkLabels() =
        runTest(timeout = 60.seconds) {
            val detail = tripDetail()
            val preparer = preparer()
            val first = preparer.prepare(detail, emptyList(), Language.En, TimeFormat.Value24, emptyList())
            preparer.prepare(detail, emptyList(), Language.En, TimeFormat.Value24, emptyList())
            assertThat(preparer.layoutCount).isEqualTo(1)
            val block = detail.plan.blocks.first { it.actionable && it.checkKey != null && !isOptionalStep(it) }
            val mark = CheckMark(checkNotNull(block.checkKey), CheckStatus.Done, detail.plan.days[0].startUtc)
            val changed = preparer.prepare(detail, emptyList(), Language.Nl, TimeFormat.Value12, listOf(mark))
            assertThat(preparer.layoutCount).isEqualTo(1)
            assertThat(changed.days.size).isEqualTo(first.days.size)
            assertThat(
                changed.days
                    .flatMap { it.blocks }
                    .first { it.id == block.id }
                    .status,
            ).isEqualTo(CheckStatus.Done)
            assertThat(PageCopy(Language.Nl, testPageCopy)("Go to a day")).isEqualTo("Ga naar een dag")
            preparer.evict(detail.trip.id)
            preparer.prepare(detail, emptyList(), Language.En, TimeFormat.Value24, emptyList())
            assertThat(preparer.layoutCount).isEqualTo(2)
        }

    @Test fun stateGatingTable() {
        val problems = mutableListOf<String>()
        for (keep in listOf(false, true)) {
            for (stored in listOf(false, true)) {
                for (online in listOf(false, true)) {
                    for (awaiting in listOf(false, true)) {
                        for (fetch in TripPageFetch.entries) {
                            val result =
                                TripPageState.resolve(
                                    keep,
                                    stored,
                                    online = online,
                                    fetch = fetch,
                                    awaitingSync = awaiting,
                                )
                            val expected =
                                when {
                                    stored -> TripPageState.Ready
                                    keep -> TripPageState.Loading
                                    !online || fetch == TripPageFetch.Offline -> TripPageState.RemoteOffline
                                    fetch == TripPageFetch.Failed -> TripPageState.Server
                                    fetch == TripPageFetch.NotFound -> TripPageState.NotFound
                                    else -> TripPageState.RemoteLoading
                                }
                            if (result != expected) problems += "$keep $stored $online $awaiting $fetch: $result"
                        }
                    }
                }
            }
        }
        assertThat(problems).isEqualTo(emptyList<String>())
        assertThat(TripPageState.resolve(true, true, noPlan = true, online = false, fetch = TripPageFetch.Failed))
            .isEqualTo(TripPageState.NoPlan)
        assertThat(TripPageState.resolve(true, true, online = false, fetch = TripPageFetch.Idle, tombstoned = true))
            .isEqualTo(TripPageState.NotFound)
        assertThat(
            TripPageState.resolve(false, false, online = false, fetch = TripPageFetch.Idle, missingAfterSync = true),
        ).isEqualTo(TripPageState.NotFound)
        val awaiting =
            TripPageState.resolve(
                false,
                false,
                online = true,
                fetch = TripPageFetch.Idle,
                missingAfterSync = true,
                awaitingSync = true,
            )
        assertThat(awaiting).isEqualTo(TripPageState.RemoteLoading)
    }

    @Test fun openingOrderAndReplacement() {
        val detail = tripDetail()
        val days = detail.plan.days
        val today = planDate(days[2].startUtc)!!

        fun open(
            day: String? = null,
            block: String? = null,
            kept: String? = null,
            now: Instant = today,
        ): DayView? = openingDay(days, detail.plan.blocks, day, block, kept, now)
        assertThat(open(day = days[0].id, kept = days[1].id)?.id).isEqualTo(days[0].id)
        val block =
            detail.plan.blocks.first {
                planDate(it.startUtc)!! >= today && planDate(it.startUtc)!! < planDate(days[2].endUtc)!!
            }
        assertThat(open(block = block.id, kept = days[0].id)?.id).isEqualTo(days[2].id)
        assertThat(open(kept = days[1].id)?.id).isEqualTo(days[1].id)
        assertThat(open()?.id).isEqualTo(days[2].id)
        assertThat(open(now = planDate(days.last().endUtc)!!)?.id).isEqualTo(days.last().id)
        assertThat(open(now = planDate(days[0].startUtc)!!.minusSeconds(60))?.id).isEqualTo(days[0].id)
        val renamed = days.toMutableList().also { it[2] = it[2].copy(id = "replacement") }
        val anchor = TripPageAnchor(days[2].id, days[2].labelDate, 180.0)
        assertThat(replacementAnchor(anchor, renamed).dayID).isEqualTo("replacement")
        assertThat(replacementAnchor(anchor, renamed).offset).isEqualTo(180.0)
        val duplicate = days.toMutableList().also { it[0] = it[0].copy(labelDate = days[2].labelDate) }
        assertThat(replacementAnchor(anchor, duplicate).dayID).isEqualTo(days[2].id)
        assertThat(replacementAnchor(anchor, renamed.takeLast(1)).dayID).isEqualTo(renamed.last().id)
    }
}
